package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// 统一互动模块 DTO（接口文档 v3.0 第八章）
// 评论/收藏/点赞/转发统一为多态接口，用 targetType 区分：1文章 2视频课程 3评论
// ---------------------------------------------------------------------------

/** 8.1 顶级评论项 */
@Serializable
data class CommentDto(
    val commentId: Long,
    val userId: Long = 0,
    val userName: String = "",
    val userAvatar: String = "",
    val content: String = "",
    val likeCount: Int = 0,
    val replyCount: Int = 0,
    val isLiked: Boolean = false,
    val createTime: String = "",
    /** 最热 2 条回复预览 */
    val hotReplies: List<HotReplyDto> = emptyList()
)

@Serializable
data class HotReplyDto(
    val commentId: Long = 0,
    val userName: String = "",
    val replyToName: String? = null,
    val content: String = ""
)

/** 8.2 楼中楼回复项 */
@Serializable
data class ReplyDto(
    val commentId: Long,
    val userId: Long = 0,
    val userName: String = "",
    val userAvatar: String = "",
    val replyToUserId: Long? = null,
    val replyToUserName: String? = null,
    val content: String = "",
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val createTime: String = ""
)

/** 8.3 发布评论/回复 */
@Serializable
data class CreateCommentRequest(
    val targetType: Int,
    val targetId: Long,
    /** 根评论ID，盖新楼传 0 */
    val rootId: Long = 0,
    /** 直接父评论ID，盖新楼传 0 */
    val parentId: Long = 0,
    val replyToUserId: Long? = null,
    val content: String
)

@Serializable
data class CreateCommentResponse(
    val commentId: Long = 0,
    /** 1-正常发布 0-触发违禁词需审核 */
    val status: Int = 1,
    val createTime: String = ""
)

/** 8.5 / 8.10 点赞切换结果 */
@Serializable
data class LikeToggleResultDto(
    val isLiked: Boolean = false,
    val likeCount: Int = 0
)

/** 8.9 收藏切换结果 */
@Serializable
data class FavoriteToggleResultDto(val isFavorited: Boolean = false)

/** 8.9 / 8.10 切换请求 */
@Serializable
data class ToggleRequest(val targetType: Int, val targetId: Long)

/** 8.11 转发记录。platform: 1微信 2QQ 3微博 4复制链接 5其他 */
@Serializable
data class ShareRequest(
    val targetType: Int,
    val targetId: Long,
    val platform: Int
)