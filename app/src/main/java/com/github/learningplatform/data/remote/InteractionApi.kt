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
import com.github.learningplatform.data.remote.dto.TableDataInfo
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 互动模块 ApiV1 接口（点赞/收藏）。
 */
interface InteractionApi {

    // ---- 点赞/收藏 ----

    /**
     * 点赞/取消 或 收藏/取消
     * @param targetType 目标类型（1帖子 2课程 3短视频）
     * @param targetId 目标ID
     * @param type 行为类型（1点赞 2收藏）
     */
    @POST("api/v1/interaction/toggle")
    suspend fun toggle(
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long,
        @Query("type") type: String
    ): Response<ApiResponse<Map<String, Any>>>

    /**
     * 批量查询当前用户已点赞/已收藏的目标ID
     * @param targetType 目标类型
     * @param type 行为类型
     * @param targetIds 目标ID，逗号分隔
     */
    @GET("api/v1/interaction/status")
    suspend fun getStatus(
        @Query("targetType") targetType: String,
        @Query("type") type: String,
        @Query("targetIds") targetIds: String
    ): Response<ApiResponse<Map<String, List<Long>>>>

    /**
     * 查某个目标的真实互动数
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @param type 行为类型
     */
    @GET("api/v1/interaction/count")
    suspend fun getCount(
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long,
        @Query("type") type: String
    ): Response<ApiResponse<Map<String, Int>>>

    // ---- 评论 ----

    /**
     * 发表评论 / 回复评论
     * @param targetType 目标类型（1帖子 2课程 3短视频）
     * @param targetId 目标ID
     * @param content 评论内容
     * @param parentId 被回复的评论ID（可选）
     */
    @POST("api/v1/comment/add")
    suspend fun addComment(
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long,
        @Query("content") content: String,
        @Query("parentId") parentId: Long? = null
    ): Response<ApiResponse<Map<String, Any>>>

    /**
     * 评论列表（分页）
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     */
    @GET("api/v1/comment/list")
    suspend fun getCommentList(
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<TableDataInfo<CommentDto>>

    /**
     * 删除评论
     * @param commentId 评论ID
     */
    @POST("api/v1/comment/remove")
    suspend fun removeComment(
        @Query("commentId") commentId: Long
    ): Response<ApiResponse<Map<String, Any>>>
}
