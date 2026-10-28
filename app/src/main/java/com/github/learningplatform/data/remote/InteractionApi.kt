package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.RawTableData
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 互动模块 ApiV1 接口（对齐后端 ApiV1InteractionController / ApiV1CommentController）。
 *
 * 后端 `AjaxResult` 把业务字段平铺在顶层（`{code,msg,active,count}`），
 * 统一用 [com.github.learningplatform.core.network.okBody] 解析。
 */
interface InteractionApi {

    // ---- 点赞/收藏 ----

    /**
     * 点赞/取消 或 收藏/取消
     * @param targetType 1帖子 2课程 3短视频
     * @param type 1点赞 2收藏
     * @return 平铺字段 active(是否已操作) / count(最新计数)
     */
    @POST("api/v1/interaction/toggle")
    suspend fun toggle(
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long,
        @Query("type") type: String
    ): Response<JsonObject>

    /**
     * 批量查询已点赞/已收藏的目标ID
     * @return 平铺字段 ids
     */
    @GET("api/v1/interaction/status")
    suspend fun getStatus(
        @Query("targetType") targetType: String,
        @Query("type") type: String,
        @Query("targetIds") targetIds: String
    ): Response<JsonObject>

    /**
     * 查某个目标的真实互动数
     * @return 平铺字段 count
     */
    @GET("api/v1/interaction/count")
    suspend fun getCount(
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long,
        @Query("type") type: String
    ): Response<JsonObject>

    // ---- 评论 ----

    /**
     * 发表评论 / 回复评论
     * @return 平铺字段 commentId / count
     */
    @POST("api/v1/comment/add")
    suspend fun addComment(
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long,
        @Query("content") content: String,
        @Query("parentId") parentId: Long? = null
    ): Response<JsonObject>

    /** 评论列表（分页），rows 为后端 Comment 结构 */
    @GET("api/v1/comment/list")
    suspend fun getCommentList(
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<RawTableData>

    /**
     * 删除评论
     * @return 平铺字段 count
     */
    @POST("api/v1/comment/remove")
    suspend fun removeComment(
        @Query("commentId") commentId: Long
    ): Response<JsonObject>
}
