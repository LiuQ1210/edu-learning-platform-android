package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// 课程视频模块 DTO（接口文档 v3.0 第六章）
// 注意：v3 为 course + course_chapter + course_lesson 三层结构
// ---------------------------------------------------------------------------

/** 课程列表项（对应后端 learning_course 表） */
@Serializable
data class CourseDto(
    // 后端实际字段
    val courseId: Long = 0,
    val courseName: String = "",
    val categoryId: Long = 0,
    val coverUrl: String = "",
    val intro: String = "",
    val likeCount: Int = 0,
    val favoriteCount: Int = 0,
    val commentCount: Int = 0,
    val ratingCount: Int = 0,
    val ratingScore: Int = 0,
    val status: String = "0",
    val categoryName: String = "",
    val avgScore: Double? = null,
    /** 后端 createTime：yyyy-MM-dd HH:mm:ss，「最新」排序与卡片日期都用它 */
    val createTime: String = "",
    /** 后端 updateTime：yyyy-MM-dd HH:mm:ss */
    val updateTime: String = "",
    // 前端兼容字段（UI 还在用，后续慢慢替换）
    val title: String = courseName,
    val videoType: Int = 1,
    val duration: Int = 0,
    val instructorName: String = "",
    val averageScore: Double = avgScore ?: 0.0,
    val studentCount: Int = 0,
    val viewCount: Int = 0,
    val shareCount: Int = 0
)

/** 6.2 课程详情（不含真实播放地址） */
@Serializable
data class CourseDetailDto(
    val courseId: Long,
    val title: String = "",
    val coverUrl: String = "",
    val videoType: Int = 1,
    val description: String = "",
    val instructorName: String = "",
    val duration: Int = 0,
    val totalLessons: Int = 0,
    val averageScore: Double = 0.0,
    val ratingCount: Int = 0,
    val studentCount: Int = 0,
    val viewCount: Int = 0,
    /** 是否免费：1是 0否 */
    val isFree: Int = 0,
    val isJoined: Boolean = false,
    val chapters: List<CourseChapterDto> = emptyList()
)

@Serializable
data class CourseChapterDto(
    val chapterId: Long,
    val title: String = "",
    val lessons: List<CourseLessonDto> = emptyList()
)

@Serializable
data class CourseLessonDto(
    val lessonId: Long,
    val title: String = "",
    val duration: Int = 0,
    val isFree: Int = 0,
    /** 转码状态：1成功 */
    val transcodeStatus: Int = 1,
    val progressSeconds: Int = 0,
    val isFinished: Int = 0
)

/** 6.3 播放凭证（VOD 限时签名 URL + 动态水印） */
@Serializable
data class PlayInfoDto(
    val courseId: Long = 0,
    val lessonId: Long = 0,
    val playUrl: String = "",
    val expireTime: Long = 0,
    val watermarkText: String = "",
    val duration: Int = 0
)

/** 6.4 播放进度心跳 */
@Serializable
data class ProgressSyncRequest(
    val courseId: Long,
    /** 长视频必传 */
    val lessonId: Long? = null,
    val progressSeconds: Int,
    val watchDuration: Int,
    val isFinished: Int,
    /** 1-Web 2-iOS 3-Android */
    val deviceType: Int = 3
)

/** 6.5 课程评分（0.5-5.0，步长0.5） */
@Serializable
data class RatingRequest(val score: Double)

@Serializable
data class RatingResultDto(
    val averageScore: Double = 0.0,
    val ratingCount: Int = 0
)

/** 6.6 评分列表项 */
@Serializable
data class RatingDto(
    val userId: Long = 0,
    val userName: String = "",
    val userAvatar: String = "",
    val score: Double = 0.0,
    val comment: String = "",
    val createTime: String = ""
)