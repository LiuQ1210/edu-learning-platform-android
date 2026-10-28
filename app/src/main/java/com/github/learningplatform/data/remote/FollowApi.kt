package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.RawTableData
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 关注模块 ApiV1 接口（对齐后端 ApiV1FollowController）。
 *
 * 后端返回 RuoYi `AjaxResult`，业务字段**平铺在顶层**（如 `{code,msg,ids:[...]}`），
 * 所以这里返回 [JsonObject]，由 [com.github.learningplatform.core.network.okBody] 统一解析。
 */
interface FollowApi {

    /** 关注某人 */
    @POST("api/v1/follow/add")
    suspend fun addFollow(
        @Query("targetUserId") targetUserId: Long
    ): Response<JsonObject>

    /** 取消关注 */
    @POST("api/v1/follow/cancel")
    suspend fun cancelFollow(
        @Query("targetUserId") targetUserId: Long
    ): Response<JsonObject>

    /** 批量查询已关注的用户ID */
    @GET("api/v1/follow/status")
    suspend fun getFollowStatus(
        @Query("userIds") userIds: String
    ): Response<JsonObject>

    /** 查某用户的粉丝数 + 关注数 */
    @GET("api/v1/follow/count")
    suspend fun getFollowCount(
        @Query("userId") userId: Long? = null
    ): Response<JsonObject>

    /** 我关注的人（分页） */
    @GET("api/v1/follow/following")
    suspend fun getFollowingList(
        @Query("userId") userId: Long? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<RawTableData>

    /** 我的粉丝（分页） */
    @GET("api/v1/follow/fans")
    suspend fun getFansList(
        @Query("userId") userId: Long? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<RawTableData>
}
