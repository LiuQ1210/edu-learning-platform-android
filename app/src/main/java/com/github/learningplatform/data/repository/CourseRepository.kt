package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.core.network.safeApiCallForUnit
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.preview
import com.github.learningplatform.data.demo.previewUnit
import com.github.learningplatform.data.local.CacheKeys
import com.github.learningplatform.data.local.CacheStore
import com.github.learningplatform.data.local.CacheTtl
import com.github.learningplatform.data.local.cachedObject
import com.github.learningplatform.data.remote.CourseApi
import com.github.learningplatform.data.remote.dto.CourseDetailDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.PlayInfoDto
import com.github.learningplatform.data.remote.dto.ProgressSyncRequest
import com.github.learningplatform.data.remote.dto.RatingDto
import com.github.learningplatform.data.remote.dto.RatingRequest
import com.github.learningplatform.data.remote.dto.RatingResultDto
import javax.inject.Inject
import javax.inject.Singleton

/** 课程视频仓库（接口文档 v3.0 第六章） */
@Singleton
class CourseRepository @Inject constructor(
    private val courseApi: CourseApi,
    private val cache: CacheStore
) {

    /**
     * 课程列表（6.1）。
     *
     * @param videoType 1 短视频 2 长视频
     * @param sort 1 最新 2 最热 3 评分
     * @param forceRefresh 下拉刷新传 true，跳过缓存强制回源
     */
    suspend fun getCourses(
        pageNum: Int = 1,
        pageSize: Int = 20,
        categoryId: Long? = null,
        videoType: Int? = null,
        sort: Int? = null,
        forceRefresh: Boolean = false
    ): PageData<CourseDto> {
        preview { return DemoData.coursePage(pageNum, pageSize, videoType) }

        // 只缓存第 1 页；翻页直接走网络
        if (pageNum != 1) {
            return safeApiCall {
                courseApi.getCourses(pageNum, pageSize, categoryId, videoType, sort)
            }
        }

        if (forceRefresh) cache.invalidateModule(MODULE)

        /*
         * 键必须带上全部影响结果的参数。
         * 首页会同时请求 sort=1（热门）和 sort=2（排行），漏掉 sort 会让两者互相覆盖 ——
         * 表现是「热度排行里出现热门课程」，而且不报错，很难查。
         */
        val key = CacheKeys.page(
            module = MODULE,
            "categoryId" to categoryId,
            "videoType" to videoType,
            "sort" to sort,
            "size" to pageSize
        )

        // 缓存整个 PageData：调用方用 total 判断 hasMore，
        // 只缓存 list 会让上拉加载失效（详见 CategoryRepository 的说明）。
        return cache.cachedObject(
            key = key,
            ttlMillis = CacheTtl.MEDIUM
        ) {
            safeApiCall {
                courseApi.getCourses(pageNum, pageSize, categoryId, videoType, sort)
            }
        }
    }

    /** 课程详情（6.2）：变动慢，用 LOOSE 档 */
    suspend fun getCourseDetail(courseId: Long, forceRefresh: Boolean = false): CourseDetailDto {
        preview { return DemoData.courseDetail(courseId) }

        val key = CacheKeys.detail("course", courseId)
        if (forceRefresh) cache.remove(key)

        return cache.cachedObject(
            key = key,
            ttlMillis = CacheTtl.LOOSE
        ) {
            safeApiCall { courseApi.getCourseDetail(courseId) }
        }
    }

    /**
     * 播放凭证：校验权限后返回带签名的**限时**播放地址。
     *
     * **不缓存，也不能缓存**：
     *  - 地址带签名且有有效期，缓存下来过了有效期播放必然失败
     *  - 取凭证本身就是一次权限校验，缓存等于绕过校验
     */
    suspend fun getPlayInfo(courseId: Long, lessonId: Long? = null): PlayInfoDto {
        preview { return DemoData.playInfo(courseId, lessonId) }
        return safeApiCall { courseApi.getPlayInfo(courseId, lessonId) }
    }

    /** 播放进度心跳（建议 15-30 秒一次）：写操作，不缓存 */
    suspend fun syncProgress(request: ProgressSyncRequest) {
        previewUnit { return }
        safeApiCallForUnit { courseApi.syncProgress(request) }
    }

    /** 评分：写操作；成功后让详情与评分列表失效，避免展示旧分数 */
    suspend fun rateCourse(courseId: Long, score: Double): RatingResultDto {
        preview { return DemoData.rateResult(score) }
        val result = safeApiCall { courseApi.rateCourse(courseId, RatingRequest(score)) }
        cache.remove(CacheKeys.detail("course", courseId))
        cache.invalidateModule(MODULE_RATING)
        return result
    }

    /** 课程评分列表：用户随时可评，变动快，用 TIGHT 档 */
    suspend fun getRatings(courseId: Long, pageNum: Int = 1, pageSize: Int = 20): PageData<RatingDto> {
        preview { return DemoData.ratings(courseId) }

        if (pageNum != 1) {
            return safeApiCall { courseApi.getRatings(courseId, pageNum, pageSize) }
        }

        return cache.cachedObject(
            key = CacheKeys.page(MODULE_RATING, "courseId" to courseId, "size" to pageSize),
            ttlMillis = CacheTtl.TIGHT
        ) {
            safeApiCall { courseApi.getRatings(courseId, pageNum, pageSize) }
        }
    }

    private companion object {
        const val MODULE = "course_list"
        const val MODULE_RATING = "course_rating"
    }
}
