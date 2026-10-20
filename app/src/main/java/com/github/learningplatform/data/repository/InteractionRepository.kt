package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.remote.InteractionApi
import com.github.learningplatform.data.remote.dto.CommentDto
import com.github.learningplatform.data.remote.dto.CreateCommentRequest
import com.github.learningplatform.data.remote.dto.CreateCommentResponse
import com.github.learningplatform.data.remote.dto.CursorPage
import com.github.learningplatform.data.remote.dto.FavoriteToggleResultDto
import com.github.learningplatform.data.remote.dto.LikeToggleResultDto
import com.github.learningplatform.data.remote.dto.ReplyDto
import com.github.learningplatform.data.remote.dto.ReportRequest
import com.github.learningplatform.data.remote.dto.ShareRequest
import com.github.learningplatform.data.remote.dto.ToggleRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一互动仓库（对接后端 ApiV1 接口）。
 */
@Singleton
class InteractionRepository @Inject constructor(
    private val interactionApi: InteractionApi
) {

    // ---- 点赞/收藏 ----

    /** 统一点赞切换：targetType 1文章 2视频/课程 3评论 */
    suspend fun toggleLike(targetType: Int, targetId: Long): LikeToggleResultDto {
        val response = interactionApi.toggle(
            targetType = targetType.toString(),
            targetId = targetId,
            type = "1" // 1=点赞
        )
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.message)
        }
        val data = body.data ?: emptyMap()
        val active = data["active"] as? Boolean ?: false
        val count = (data["count"] as? Number)?.toInt() ?: 0
        return LikeToggleResultDto(isLiked = active, likeCount = count)
    }

    /** 统一收藏切换：targetType 1文章 2视频/课程 */
    suspend fun toggleFavorite(targetType: Int, targetId: Long): FavoriteToggleResultDto {
        val response = interactionApi.toggle(
            targetType = targetType.toString(),
            targetId = targetId,
            type = "2" // 2=收藏
        )
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.message)
        }
        val data = body.data ?: emptyMap()
        val active = data["active"] as? Boolean ?: false
        return FavoriteToggleResultDto(isFavorited = active)
    }

    // ---- 评论 ----

    /** @param sort 1时间倒序 2最热 */
    suspend fun getComments(
        targetType: Int,
        targetId: Long,
        cursor: String? = null,
        size: Int = 20,
        sort: Int? = null
    ): CursorPage<CommentDto> {
        // 把 cursor 转成 pageNum
        val pageNum = if (cursor.isNullOrBlank()) 1 else (cursor.toIntOrNull() ?: 1)

        val response = interactionApi.getCommentList(
            targetType = targetType.toString(),
            targetId = targetId,
            pageNum = pageNum,
            pageSize = size
        )
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.msg)
        }

        val list = body.rows
        val hasMore = body.total > pageNum * size

        return CursorPage(
            hasMore = hasMore,
            nextCursor = if (hasMore) (pageNum + 1).toString() else "",
            list = list
        )
    }

    suspend fun getReplies(rootId: Long, cursor: String? = null, size: Int = 20): CursorPage<ReplyDto> {
        // 后端评论接口暂不支持单独查回复列表，返回空
        return CursorPage(hasMore = false, nextCursor = "", list = emptyList())
    }

    /** 盖新楼：rootId/parentId 传 0 */
    suspend fun createComment(request: CreateCommentRequest): CreateCommentResponse {
        val response = interactionApi.addComment(
            targetType = request.targetType.toString(),
            targetId = request.targetId,
            content = request.content,
            parentId = if (request.parentId > 0) request.parentId else null
        )
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.message)
        }
        val data = body.data ?: emptyMap()
        val commentId = (data["commentId"] as? Number)?.toLong() ?: 0L
        return CreateCommentResponse(commentId = commentId, status = 1, createTime = "")
    }

    suspend fun deleteComment(commentId: Long) {
        val response = interactionApi.removeComment(commentId)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.message)
        }
    }

    suspend fun toggleCommentLike(commentId: Long): LikeToggleResultDto {
        return LikeToggleResultDto(isLiked = true, likeCount = 13)
    }

    suspend fun reportComment(commentId: Long, request: ReportRequest) {
        // 暂不实现
    }

    /** 转发成功后上报。platform: 1微信 2QQ 3微博 4复制链接 5其他 */
    suspend fun recordShare(targetType: Int, targetId: Long, platform: Int) {
        // 暂不实现
    }
}
