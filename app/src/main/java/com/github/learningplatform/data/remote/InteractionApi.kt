package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 统一互动模块（接口文档 v3.0 第八章，12 个接口）。
 *
 * 评论采用游标分页（cursor + size），三级楼中楼由 rootId + parentId 表达。
 * 收藏/点赞为统一多态 toggle 接口，写操作均带 X-Idempotent-Token（拦截器自动注入）。
 */
interface InteractionApi {

    /** 8.1 顶级评论列表（sort: 1时间倒序 2最热） */
    @GET("comments")
    suspend fun getComments(
        @Query("targetType") targetType: Int,
        @Query("targetId") targetId: Long,
        @Query("cursor") cursor: String? = null,
        @Query("size") size: Int = 20,
        @Query("sort") sort: Int? = null
    ): Response<ApiResponse<CursorPage<CommentDto>>>

    /** 8.2 某顶级评论下的全部回复 */
    @GET("comments/{rootId}/replies")
    suspend fun getReplies(
        @Path("rootId") rootId: Long,
        @Query("cursor") cursor: String? = null,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<CursorPage<ReplyDto>>>

    /** 8.3 发布评论 / 回复 */
    @POST("comments")
    suspend fun createComment(
        @Body body: CreateCommentRequest
    ): Response<ApiResponse<CreateCommentResponse>>

    /** 8.4 删除评论（本人或管理员） */
    @DELETE("comments/{commentId}")
    suspend fun deleteComment(@Path("commentId") commentId: Long): Response<ApiResponse<Unit>>

    /** 8.5 评论点赞切换 */
    @POST("comments/{commentId}/like/toggle")
    suspend fun toggleCommentLike(
        @Path("commentId") commentId: Long
    ): Response<ApiResponse<LikeToggleResultDto>>

    /** 8.6 举报评论 */
    @POST("comments/{commentId}/report")
    suspend fun reportComment(
        @Path("commentId") commentId: Long,
        @Body body: ReportRequest
    ): Response<ApiResponse<Unit>>

    /** 8.9 收藏切换（文章/课程） */
    @POST("favorites/toggle")
    suspend fun toggleFavorite(
        @Body body: ToggleRequest
    ): Response<ApiResponse<FavoriteToggleResultDto>>

    /** 8.10 点赞切换（文章/课程/评论） */
    @POST("likes/toggle")
    suspend fun toggleLike(
        @Body body: ToggleRequest
    ): Response<ApiResponse<LikeToggleResultDto>>

    /** 8.11 记录转发行为 */
    @POST("shares")
    suspend fun recordShare(@Body body: ShareRequest): Response<ApiResponse<Unit>>
}