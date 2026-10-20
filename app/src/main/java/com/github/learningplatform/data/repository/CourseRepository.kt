package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.local.CacheKeys
import com.github.learningplatform.data.local.CacheStore
import com.github.learningplatform.data.local.CacheTtl
import com.github.learningplatform.data.local.cachedObject
import com.github.learningplatform.data.remote.CourseApi
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.TableDataInfo
import javax.inject.Inject
import javax.inject.Singleton

/** 课程仓库（对接后端 ApiV1 接口） */
@Singleton
class CourseRepository @Inject constructor(
    private val courseApi: CourseApi,
    private val cache: CacheStore
) {

    /**
     * 课程列表
     * @param categoryId 分类ID
     * @param videoType 视频类型（兼容旧参数，后端暂不支持）
     * @param sort 排序（兼容旧参数：1最新 2最热 3评分，后端用 sortBy）
     * @param sortBy 排序：likeCount / createTime / avgScore
     * @param forceRefresh 下拉刷新传 true
     */
    suspend fun getCourses(
        pageNum: Int = 1,
        pageSize: Int = 20,
        categoryId: Long? = null,
        videoType: Int? = null,
        sort: Int? = null,
        sortBy: String? = null,
        forceRefresh: Boolean = false
    ): PageData<CourseDto> {
        // 兼容旧参数：把 sort（1最新 2最热 3评分）转成 sortBy
        val actualSortBy = sortBy ?: when (sort) {
            2 -> "likeCount"
            3 -> "avgScore"
            else -> "createTime"
        }

        // 只缓存第 1 页；翻页直接走网络
        if (pageNum != 1) {
            return fetchCourseList(pageNum, pageSize, categoryId, actualSortBy)
        }

        if (forceRefresh) cache.invalidateModule(MODULE)

        val key = CacheKeys.page(
            module = MODULE,
            "categoryId" to categoryId,
            "sortBy" to actualSortBy,
            "size" to pageSize
        )

        return cache.cachedObject(
            key = key,
            ttlMillis = CacheTtl.MEDIUM
        ) {
            fetchCourseList(pageNum, pageSize, categoryId, actualSortBy)
        }
    }

    private suspend fun fetchCourseList(
        pageNum: Int,
        pageSize: Int,
        categoryId: Long?,
        sortBy: String?
    ): PageData<CourseDto> {
        val response = courseApi.getCourseList(
            pageNum = pageNum,
            pageSize = pageSize,
            categoryId = categoryId,
            sortBy = sortBy
        )
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()} ${response.message()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.msg)
        }
        return PageData(total = body.total, list = body.rows)
    }

    /** 课程详情（后端暂未提供，返回假数据） */
    suspend fun getCourseDetail(courseId: Long, forceRefresh: Boolean = false): com.github.learningplatform.data.remote.dto.CourseDetailDto {
        return DemoData.courseDetail(courseId)
    }

    /** 播放凭证（后端暂未提供，返回假数据） */
    suspend fun getPlayInfo(courseId: Long, lessonId: Long? = null): com.github.learningplatform.data.remote.dto.PlayInfoDto {
        return DemoData.playInfo(courseId, lessonId)
    }

    /** 播放进度心跳（后端暂未提供） */
    suspend fun syncProgress(request: com.github.learningplatform.data.remote.dto.ProgressSyncRequest) {
        // 暂不实现
    }

    /** 评分（后端暂未提供，返回假数据） */
    suspend fun rateCourse(courseId: Long, score: Double): com.github.learningplatform.data.remote.dto.RatingResultDto {
        return DemoData.rateResult(score)
    }

    /** 课程评分列表（后端暂未提供，返回假数据） */
    suspend fun getRatings(courseId: Long, pageNum: Int = 1, pageSize: Int = 20): PageData<com.github.learningplatform.data.remote.dto.RatingDto> {
        return DemoData.ratings(courseId)
    }

    private companion object {
        const val MODULE = "course_list"
    }
}
