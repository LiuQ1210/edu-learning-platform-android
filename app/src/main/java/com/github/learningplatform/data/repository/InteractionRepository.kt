package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.RawTableData
import com.github.learningplatform.core.network.bool
import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.longList
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.ApiTargetType
import com.github.learningplatform.data.remote.InteractionApi
import com.github.learningplatform.data.remote.dto.CommentDto
import com.github.learningplatform.data.remote.dto.CreateCommentRequest
import com.github.learningplatform.data.remote.dto.CreateCommentResponse
import com.github.learningplatform.data.remote.dto.CursorPage
import com.github.learningplatform.data.remote.dto.FavoriteToggleResultDto
import com.github.learningplatform.data.remote.dto.HotReplyDto
import com.github.learningplatform.data.remote.dto.LikeToggleResultDto
import com.github.learningplatform.data.remote.dto.ReplyDto
import com.github.learningplatform.data.remote.dto.ReportRequest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一互动仓库（对接后端 ApiV1 接口）。
 *
 * 后端把业务字段**平铺**在 `AjaxResult` 顶层（`{code,msg,active,count}`），
 * 因此统一走 [okBody] + [payload] 解析，不再按 `data` 取。
 */
@Singleton
class InteractionRepository @Inject constructor(
    private val interactionApi: InteractionApi
) {

    // ---- 点赞/收藏 ----

    /** 统一点赞切换：targetType 1文章 2视频/课程 3评论 */
    suspend fun toggleLike(targetType: Int, targetId: Long): LikeToggleResultDto {
        val data = interactionApi.toggle(
            targetType = ApiTargetType.ofNumeric(targetType) ?: "1",
            targetId = targetId,
            type = "1" // 1=点赞
        ).okBody().payload()
        return LikeToggleResultDto(
            isLiked = data.bool("active"),
            likeCount = data.int("count")
        )
    }

    /** 统一收藏切换：targetType 1文章 2视频/课程 */
    suspend fun toggleFavorite(targetType: Int, targetId: Long): FavoriteToggleResultDto {
        val data = interactionApi.toggle(
            targetType = ApiTargetType.ofNumeric(targetType) ?: "1",
            targetId = targetId,
            type = "2" // 2=收藏
        ).okBody().payload()
        return FavoriteToggleResultDto(isFavorited = data.bool("active"))
    }

    /** 查询当前用户在一批目标上是否已点赞/收藏 */
    suspend fun getActiveIds(targetType: Int, type: Int, targetIds: List<Long>): List<Long> {
        if (targetIds.isEmpty()) return emptyList()
        val body = interactionApi.getStatus(
            targetType = ApiTargetType.ofNumeric(targetType) ?: "1",
            type = type.toString(),
            targetIds = targetIds.joinToString(",")
        ).okBody()
        return body.longList("ids").ifEmpty { body.payload().longList("ids") }
    }

    /** 查某个目标的真实互动数 */
    suspend fun getInteractionCount(targetType: Int, targetId: Long, type: Int): Int =
        interactionApi.getCount(
            targetType = ApiTargetType.ofNumeric(targetType) ?: "1",
            targetId = targetId,
            type = type.toString()
        ).okBody().payload().int("count")

    // ---- 评论 ----

    /** @param sort 1时间倒序 2最热（后端当前按时间，忽略 sort） */
    suspend fun getComments(
        targetType: Int,
        targetId: Long,
        cursor: String? = null,
        size: Int = 20,
        sort: Int? = null
    ): CursorPage<CommentDto> {
        val pageNum = if (cursor.isNullOrBlank()) 1 else (cursor.toIntOrNull() ?: 1)
        val table = interactionApi.getCommentList(
            targetType = ApiTargetType.ofNumeric(targetType) ?: "1",
            targetId = targetId,
            pageNum = pageNum,
            pageSize = size
        ).okTable()

        val list = table.rows.map { it.toComment() }
        val hasMore = table.total > pageNum.toLong() * size

        return CursorPage(
            hasMore = hasMore,
            nextCursor = if (hasMore) (pageNum + 1).toString() else "",
            list = list
        )
    }

    /** 后端评论接口未提供单独的回复列表，先返回空 */
    suspend fun getReplies(rootId: Long, cursor: String? = null, size: Int = 20): CursorPage<ReplyDto> =
        CursorPage(hasMore = false, nextCursor = "", list = emptyList())

    /** 盖新楼：rootId/parentId 传 0 */
    suspend fun createComment(request: CreateCommentRequest): CreateCommentResponse {
        val data = interactionApi.addComment(
            targetType = ApiTargetType.ofNumeric(request.targetType) ?: "1",
            targetId = request.targetId,
            content = request.content,
            parentId = if (request.parentId > 0) request.parentId else null
        ).okBody().payload()
        return CreateCommentResponse(
            commentId = data.long("commentId"),
            status = 1,
            createTime = ""
        )
    }

    suspend fun deleteComment(commentId: Long) {
        interactionApi.removeComment(commentId).okBody()
    }

    suspend fun toggleCommentLike(commentId: Long): LikeToggleResultDto {
        // 后端 comment 表没有点赞字段，暂用本地点赞态占位
        return LikeToggleResultDto(isLiked = true, likeCount = 13)
    }

    suspend fun reportComment(commentId: Long, request: ReportRequest) {
        // 后端未提供评论举报接口
    }

    /** 转发成功后上报。platform: 1微信 2QQ 3微博 4复制链接 5其他（后端未提供） */
    suspend fun recordShare(targetType: Int, targetId: Long, platform: Int) {
    }
}

/**
 * 后端 Comment 行 -> 客户端 CommentDto。
 *
 * 后端把二级回复挂在根评论的 `children` 数组里（不再单独提供回复列表接口），
 * 而且**不返回 `replyCount`**——之前这里没解析 children，导致：
 *  - 真正的二级回复永远显示不出来
 *  - replyCount 恒为 0，UI 上的「查看 N 条回复」永远不出现
 * 现在把 children 解析成 hotReplies，并用其长度兜底 replyCount。
 */
private fun JsonObject.toComment(): CommentDto {
    val children = (this["children"] as? JsonArray) ?: JsonArray(emptyList())
    val replies = children.mapNotNull { it as? JsonObject }.map { child ->
        HotReplyDto(
            commentId = child.long("commentId"),
            userName = child.str("userName"),
            // 后端字段叫 replyUserName：被回复人的昵称
            replyToName = child.str("replyUserName").ifBlank { null },
            content = child.str("content")
        )
    }
    val backendCount = int("replyCount")
    return CommentDto(
        commentId = long("commentId"),
        userId = long("userId"),
        userName = str("userName"),
        userAvatar = str("userAvatar").ifEmpty { str("avatar") },
        content = str("content"),
        likeCount = int("likeCount"),
        replyCount = if (backendCount > 0) backendCount else replies.size,
        isLiked = bool("isLiked"),
        createTime = str("createTime"),
        hotReplies = replies
    )
}
