package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.core.network.double
import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.local.CacheKeys
import com.github.learningplatform.data.local.CacheStore
import com.github.learningplatform.data.local.CacheTtl
import com.github.learningplatform.data.local.cachedObject
import com.github.learningplatform.data.remote.CourseApi
import com.github.learningplatform.data.remote.dto.CourseChapterDto
import com.github.learningplatform.data.remote.dto.CourseDetailDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.CourseLessonDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.TableDataInfo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
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

    /**
     * 课程详情（后端 `GET /api/v1/course/{courseId}`）。
     *
     * 后端返回 `{code, msg, data:{ course, chapters:[{...,videos:[]}], videos:[] }}`，
     * 这里映射成 UI 使用的 [CourseDetailDto]：
     *  - course      → 标题 / 封面 / 简介 / 各类计数
     *  - chapters    → 章节，每章的视频作为 lessons
     *  - data.videos → 不属于任何章节的课时，单独放进 lessons
     *
     * 失败时直接抛给上层（详情页有 ErrorState + 重试），不再静默用样例数据。
     */
    suspend fun getCourseDetail(
        courseId: Long,
        forceRefresh: Boolean = false
    ): com.github.learningplatform.data.remote.dto.CourseDetailDto {
        val data = courseApi.getCourseDetail(courseId).okBody().payload()
        val course = data["course"] as? JsonObject
            ?: throw ApiException(-1, "课程不存在或已下架")

        fun JsonObject.toLesson(): CourseLessonDto = CourseLessonDto(
            lessonId = long("videoId"),
            title = str("videoTitle"),
            // 后端 learning_course_video 没有 duration 字段，暂时取 0，UI 显示「--:--」
            duration = int("duration")
        )

        val chapters = (data["chapters"] as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            ?.map { chapter ->
                CourseChapterDto(
                    chapterId = chapter.long("chapterId"),
                    title = chapter.str("chapterTitle"),
                    lessons = (chapter["videos"] as? JsonArray)
                        ?.mapNotNull { it as? JsonObject }
                        ?.map { it.toLesson() }
                        .orEmpty()
                )
            }
            .orEmpty()

        val looseLessons = (data["videos"] as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            ?.map { it.toLesson() }
            .orEmpty()

        return com.github.learningplatform.data.remote.dto.CourseDetailDto(
            courseId = course.long("courseId"),
            title = course.str("courseName"),
            coverUrl = course.str("coverUrl"),
            // 有章节或独立课时都按「长视频课程」处理，否则视为短视频
            videoType = if (chapters.isEmpty() && looseLessons.isEmpty()) 1 else 2,
            description = course.str("intro"),
            totalLessons = chapters.sumOf { it.lessons.size } + looseLessons.size,
            averageScore = course.double("avgScore"),
            ratingCount = course.int("ratingCount"),
            viewCount = course.int("viewCount"),
            likeCount = course.int("likeCount"),
            favoriteCount = course.int("favoriteCount"),
            commentCount = course.int("commentCount"),
            categoryName = course.str("categoryName"),
            chapters = chapters,
            lessons = looseLessons
        )
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
