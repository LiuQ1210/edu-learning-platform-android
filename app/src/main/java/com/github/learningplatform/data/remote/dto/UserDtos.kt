package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// 用户中心模块 DTO（接口文档 v3.0 第三章，12 个接口）
// ---------------------------------------------------------------------------

/** 3.3 他人博客主页 */
@Serializable
data class BlogHomeDto(
    val userId: Long,
    val nickname: String = "",
    val avatar: String = "",
    val bio: String = "",
    val articleCount: Int = 0,
    val totalViewCount: Int = 0,
    val totalLikeCount: Int = 0,
    val totalFavoriteCount: Int = 0,
    val articles: PageData<ArticleDto> = PageData()
)

/** 3.4 我的课程列表项 */
@Serializable
data class MyCourseDto(
    val courseId: Long,
    val title: String = "",
    val coverUrl: String = "",
    val instructorName: String = "",
    val studyProgress: Int = 0,
    val lastStudyTime: String = ""
)

/** 3.5 加入课程结果 */
@Serializable
data class JoinedResultDto(val joined: Boolean = false)

/** 3.7 收藏项 / 3.8 点赞项（统一多态结构） */
@Serializable
data class UserTargetItemDto(
    val targetType: Int = 1,
    val targetId: Long = 0,
    val title: String = "",
    val cover: String = "",
    val collectTime: String = ""
)

/** 3.9 浏览历史项 */
@Serializable
data class ViewHistoryItemDto(
    val targetType: Int = 1,
    val targetId: Long = 0,
    val title: String = "",
    val cover: String = "",
    val progressSeconds: Int = 0,
    val lastViewTime: String = ""
)

/** 3.11 下载记录 */
@Serializable
data class DownloadRecordDto(
    val id: Long,
    val resourceId: Long = 0,
    val resourceType: Int = 1,
    val resourceTitle: String = "",
    val fileName: String = "",
    val fileSize: Long = 0,
    val createTime: String = ""
)

/** 3.12 签到日历（月度统计） */
@Serializable
data class SignCalendarDto(
    val continueSignDay: Int = 0,
    val totalSignDay: Int = 0,
    val signedDates: List<String> = emptyList()
)