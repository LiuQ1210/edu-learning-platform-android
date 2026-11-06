package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.core.network.ImageUrl
import com.github.learningplatform.core.network.double
import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
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
import com.github.learningplatform.data.remote.dto.PlayInfoDto
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
    ): CourseDetailDto {
        val data = courseApi.getCourseDetail(courseId).okBody().payload()
        val course = data["course"] as? JsonObject
            ?: throw ApiException(-1, "课程不存在或已下架")

        fun JsonObject.toLesson(): CourseLessonDto = CourseLessonDto(
            lessonId = long("videoId"),
            title = str("videoTitle"),
            // 后端 learning_course_video 没有 duration 字段，暂时取 0，UI 显示「--:--」
            duration = int("duration"),
            // 播放地址随详情一起下发（后端没实现 6.3 play-info）。
            // 存的是 OSS 直链时原样返回，是 /profile/upload/... 时按 BASE_URL 补全。
            videoUrl = ImageUrl.resolve(str("videoUrl"))
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

        return CourseDetailDto(
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

    /**
     * 课程播放地址（文档 6.3 的播放凭证）。
     *
     * 后端**没有**实现 `GET /api/v1/courses/{courseId}/play-info`，但课程视频的真实
     * 地址一直随 6.2 详情下发（`chapters[].videos[].videoUrl`，来自
     * `learning_course_video.video_url`）。所以这里不调独立接口，直接把详情里的
     * [CourseLessonDto.videoUrl] 组装成 [PlayInfoDto]，和短视频模块「地址就在列表
     * 数据里」的做法保持一致。
     *
     * 之前这里返回写死的样例数据（一条公开测试流），源站下线后**整门课都播不了**：
     * 与用户选哪门课、哪一节无关，播放器一律报 404。不要再回退到任何假地址。
     *
     * 视频是 OSS 直链（实测 206 + Range，可拖动进度条）；签名 URL / 动态水印等 VOD
     * 能力等后端补上 6.3 后，只需替换本方法的实现，UI 与 DTO 都不用动。
     */
    suspend fun getPlayInfo(courseId: Long, lessonId: Long? = null): PlayInfoDto =
        playInfoOf(getCourseDetail(courseId), lessonId)

    /**
     * 从**已经取到的**课程详情里挑出目标课时的播放地址。
     *
     * 播放页本来就要拉详情拿标题，用这个重载可以避免为了一个地址再请求一遍。
     *
     * @param lessonId 长视频传小节 ID；短视频课程（无章节）传 null，取第一条有地址的课时
     * @throws ApiException 课时不存在、或后端没给该课时传视频 —— 宁可报错也不播别的
     */
    fun playInfoOf(detail: CourseDetailDto, lessonId: Long?): PlayInfoDto {
        val lessons = detail.chapters.flatMap { it.lessons } + detail.lessons
        val target = if (lessonId != null) {
            lessons.firstOrNull { it.lessonId == lessonId }
                ?: throw ApiException(-1, "课时不存在或已删除")
        } else {
            lessons.firstOrNull { it.videoUrl.isNotBlank() }
                ?: throw ApiException(-1, "该课程暂未上传视频")
        }
        if (target.videoUrl.isBlank()) {
            throw ApiException(-1, "「${target.title.ifBlank { "该课时" }}」还没有上传视频")
        }
        return PlayInfoDto(
            courseId = detail.courseId,
            lessonId = target.lessonId,
            playUrl = target.videoUrl,
            // 直链没有有效期，也不做动态水印；字段保留，等 6.3 接 VOD 后填真实值
            expireTime = 0,
            watermarkText = "",
            duration = target.duration
        )
    }

    /** 播放进度心跳（后端暂未提供） */
    suspend fun syncProgress(request: com.github.learningplatform.data.remote.dto.ProgressSyncRequest) {
        // 暂不实现
    }

    private companion object {
        const val MODULE = "course_list"
    }
}
