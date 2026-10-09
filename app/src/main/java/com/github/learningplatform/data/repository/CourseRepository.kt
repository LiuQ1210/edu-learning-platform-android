package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.core.network.safeApiCallForUnit
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.contentPreview
import com.github.learningplatform.data.demo.contentPreviewUnit
import com.github.learningplatform.data.local.CacheDao
import com.github.learningplatform.data.local.CacheEntry
import com.github.learningplatform.data.local.CacheKeys
import com.github.learningplatform.data.local.CacheTtl
import com.github.learningplatform.data.remote.CourseApi
import com.github.learningplatform.data.remote.dto.CourseDetailDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.PlayInfoDto
import com.github.learningplatform.data.remote.dto.ProgressSyncRequest
import com.github.learningplatform.data.remote.dto.RatingDto
import com.github.learningplatform.data.remote.dto.RatingResultDto
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** 课程视频仓库（接口文档 v3.0 第六章） */
@Singleton
class CourseRepository @Inject constructor(
    private val courseApi: CourseApi,
    private val cacheDao: CacheDao
) {

    /**
     * 【任务③：首页课程列表接缓存】
     *
     * 与任务②分类树同一模板（读缓存优先 / Cache-Aside）：
     *  1. UI_PREVIEW=true（编译期开关）仍走 DemoData 短路，不读不写缓存，防假数据污染；
     *  2. 真实模式下先读 cache_kv：key = course_home:page=..:size=..:cat=..:vtype=..:sort=..，
     *     命中且未过期（TTL = CacheTtl.COURSE_HOME，默认 10 分钟）→ 反序列化直接返回；
     *  3. 未命中 / 已过期 / 反序列化失败 → 回源（真实接口 6.1），成功后序列化写缓存；
     *  4. 写缓存失败（磁盘/IO 异常）不阻断主流程，下次继续回源。
     *
     * 全参数拼 key：pageNum/pageSize/categoryId/videoType/sort 缺一不可，
     * 否则分页、分类、排序之间会互相串数据（详见 CacheKeys.courseHome）。
     *
     * @param videoType 1短视频 2长视频；@param sort 1最新 2最热 3评分
     */
    suspend fun getCourses(
        pageNum: Int = 1,
        pageSize: Int = 20,
        categoryId: Long? = null,
        videoType: Int? = null,
        sort: Int? = null
    ): PageData<CourseDto> {
        contentPreview { return DemoData.coursePage(pageNum, pageSize, videoType) }

        val key = CacheKeys.courseHome(pageNum, pageSize, categoryId, videoType, sort)

        // 1) 读缓存：命中且新鲜直接返回
        cacheDao.getEntry(key)?.let { entry ->
            val fresh = System.currentTimeMillis() - entry.updatedAt <= CacheTtl.COURSE_HOME
            if (fresh) {
                runCatching { json.decodeFromString<PageData<CourseDto>>(entry.value) }
                    .getOrNull()
                    ?.let { return it }
            }
        }

        // 2) 回源：真实接口
        val page = safeApiCall {
            courseApi.getCourses(pageNum, pageSize, categoryId, videoType, sort)
        }

        // 3) 写缓存（失败不阻断主流程）
        runCatching {
            cacheDao.put(
                CacheEntry(
                    key = key,
                    value = json.encodeToString(page),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return page
    }

    /**
     * 【任务④：课程详情接缓存 + 失效逻辑（过期重取、下拉强制回源）】
     *
     * 缓存模板与任务②③一致（读缓存优先 / Cache-Aside）：
     *  1. UI_PREVIEW=true（编译期开关）仍走 DemoData 短路，不读不写缓存，防假数据污染；
     *  2. 真实模式下先读 cache_kv：key = course_detail:{courseId}，
     *     命中且未过期（TTL = CacheTtl.COURSE_DETAIL，默认 1 小时）→ 反序列化直接返回；
     *  3. 未命中 / 已过期 / 反序列化失败 → 回源（真实接口 6.2），成功后写缓存；
     *  4. 写缓存失败不阻断主流程。
     *
     * 失效逻辑：
     *  - 过期重取：由 TTL 判断自动完成——超过 1 小时再进入详情页即回源刷新，无需额外代码；
     *  - 下拉强制回源：forceRefresh = true 时跳过缓存读，直接回源并覆盖写缓存，
     *    供详情页下拉刷新手势调用（见 CourseDetailViewModel.refresh）。
     */
    suspend fun getCourseDetail(courseId: Long, forceRefresh: Boolean = false): CourseDetailDto {
        contentPreview { return DemoData.courseDetail(courseId) }

        val key = CacheKeys.courseDetail(courseId)

        // 1) 读缓存：非强制刷新且命中新鲜才直接返回
        if (!forceRefresh) {
            cacheDao.getEntry(key)?.let { entry ->
                val fresh = System.currentTimeMillis() - entry.updatedAt <= CacheTtl.COURSE_DETAIL
                if (fresh) {
                    runCatching { json.decodeFromString<CourseDetailDto>(entry.value) }
                        .getOrNull()
                        ?.let { return it }
                }
            }
        }

        // 2) 回源：真实接口（强制刷新时也走这里）
        val detail = safeApiCall { courseApi.getCourseDetail(courseId) }

        // 3) 写缓存（失败不阻断主流程）
        runCatching {
            cacheDao.put(
                CacheEntry(
                    key = key,
                    value = json.encodeToString(detail),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return detail
    }

    /** 播放凭证：校验权限后返回带签名的限时播放地址 */
    suspend fun getPlayInfo(courseId: Long, lessonId: Long? = null): PlayInfoDto {
        contentPreview { return DemoData.playInfo(courseId, lessonId) }
        return safeApiCall { courseApi.getPlayInfo(courseId, lessonId) }
    }

    /** 播放进度心跳（建议 15-30 秒一次） */
    suspend fun syncProgress(request: ProgressSyncRequest) {
        contentPreviewUnit { return }
        safeApiCallForUnit { courseApi.syncProgress(request) }
    }

    suspend fun rateCourse(courseId: Long, score: Double): RatingResultDto {
        contentPreview { return DemoData.rateResult(score) }
        return safeApiCall { courseApi.rateCourse(courseId, com.github.learningplatform.data.remote.dto.RatingRequest(score)) }
    }

    suspend fun getRatings(courseId: Long, pageNum: Int = 1, pageSize: Int = 20): PageData<RatingDto> {
        contentPreview { return DemoData.ratings(courseId) }
        return safeApiCall { courseApi.getRatings(courseId, pageNum, pageSize) }
    }

    private companion object {
        /** 缓存 JSON 序列化：与接口 DTO 同一套 kotlinx.serialization */
        private val json = Json { ignoreUnknownKeys = true }
    }
}
