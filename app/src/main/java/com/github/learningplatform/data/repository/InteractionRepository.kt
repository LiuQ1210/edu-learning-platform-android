package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.core.network.safeApiCallForUnit
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.preview
import com.github.learningplatform.data.demo.previewUnit
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
 * 统一互动仓库（接口文档 v3.0 第八章）。
 *
 * 评论为游标分页：首页 cursor 传 null，后续传上一页的 nextCursor。
 */
@Singleton
class InteractionRepository @Inject constructor(
    private val interactionApi: InteractionApi
) {

    /** @param sort 1时间倒序 2最热 */
    suspend fun getComments(
        targetType: Int,
        targetId: Long,
        cursor: String? = null,
        size: Int = 20,
        sort: Int? = null
    ): CursorPage<CommentDto> {
        preview {
            // 必须按 cursor 切页。之前固定返回同一页，ViewModel 累加后出现重复
            // commentId，LazyColumn 直接抛 "Key 88009 was already used"。
            // cursor 语义沿用后端：上一页最后一条的 ID；第一页传 null。
            val all = DemoData.commentList(targetId)
            val startIndex = if (cursor.isNullOrBlank()) {
                0
            } else {
                val idx = all.indexOfFirst { it.commentId == cursor.toLongOrNull() }
                if (idx < 0) 0 else idx + 1
            }
            val slice = all.drop(startIndex).take(size)
            val consumed = startIndex + slice.size
            val more = consumed < all.size
            return CursorPage(
                hasMore = more,
                // 没有下一页时游标给空串。给「最后一个 ID」的话，再请求一次会拿到
                // 同一批，又变回重复 key。
                nextCursor = if (more) slice.lastOrNull()?.commentId?.toString().orEmpty() else "",
                list = slice
            )
        }
        return safeApiCall {
            interactionApi.getComments(targetType, targetId, cursor, size, sort)
        }
    }

    suspend fun getReplies(rootId: Long, cursor: String? = null, size: Int = 20): CursorPage<ReplyDto> {
        preview {
            // 同样按 cursor 切页，理由见 getComments
            val all = DemoData.replies(rootId)
            val startIndex = if (cursor.isNullOrBlank()) {
                0
            } else {
                val idx = all.indexOfFirst { it.commentId == cursor.toLongOrNull() }
                if (idx < 0) 0 else idx + 1
            }
            val slice = all.drop(startIndex).take(size)
            val consumed = startIndex + slice.size
            val more = consumed < all.size
            return CursorPage(
                hasMore = more,
                nextCursor = if (more) slice.lastOrNull()?.commentId?.toString().orEmpty() else "",
                list = slice
            )
        }
        return safeApiCall { interactionApi.getReplies(rootId, cursor, size) }
    }

    /** 盖新楼：rootId/parentId 传 0 */
    suspend fun createComment(request: CreateCommentRequest): CreateCommentResponse {
        preview { return CreateCommentResponse(commentId = 88999L, status = 1, createTime = "2026-09-18 10:00:00") }
        return safeApiCall { interactionApi.createComment(request) }
    }

    suspend fun deleteComment(commentId: Long) {
        previewUnit { return }
        safeApiCallForUnit { interactionApi.deleteComment(commentId) }
    }

    suspend fun toggleCommentLike(commentId: Long): LikeToggleResultDto {
        preview { return LikeToggleResultDto(isLiked = true, likeCount = 13) }
        return safeApiCall { interactionApi.toggleCommentLike(commentId) }
    }

    suspend fun reportComment(commentId: Long, request: ReportRequest) {
        previewUnit { return }
        safeApiCallForUnit { interactionApi.reportComment(commentId, request) }
    }

    /** 统一收藏切换：targetType 1文章 2视频/课程 */
    suspend fun toggleFavorite(targetType: Int, targetId: Long): FavoriteToggleResultDto {
        preview { return FavoriteToggleResultDto(isFavorited = true) }
        return safeApiCall { interactionApi.toggleFavorite(ToggleRequest(targetType, targetId)) }
    }

    /** 统一点赞切换：targetType 1文章 2视频/课程 3评论 */
    suspend fun toggleLike(targetType: Int, targetId: Long): LikeToggleResultDto {
        preview { return LikeToggleResultDto(isLiked = true, likeCount = 51) }
        return safeApiCall { interactionApi.toggleLike(ToggleRequest(targetType, targetId)) }
    }

    /** 转发成功后上报。platform: 1微信 2QQ 3微博 4复制链接 5其他 */
    suspend fun recordShare(targetType: Int, targetId: Long, platform: Int) {
        previewUnit { return }
        safeApiCallForUnit { interactionApi.recordShare(ShareRequest(targetType, targetId, platform)) }
    }
}
