package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.TableDataInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** 关注模块 ApiV1 接口 */
interface FollowApi {

    /** 关注某人 */
    @POST("api/v1/follow/add")
    suspend fun addFollow(
        @Query("targetUserId") targetUserId: Long
    ): Response<ApiResponse<Map<String, Any>>>

    /** 取消关注 */
    @POST("api/v1/follow/cancel")
    suspend fun cancelFollow(
        @Query("targetUserId") targetUserId: Long
    ): Response<ApiResponse<Map<String, Any>>>

    /** 批量查询已关注的用户ID */
    @GET("api/v1/follow/status")
    suspend fun getFollowStatus(
        @Query("userIds") userIds: String
    ): Response<ApiResponse<Map<String, List<Long>>>>

    /** 查某用户的粉丝数 + 关注数 */
    @GET("api/v1/follow/count")
    suspend fun getFollowCount(
        @Query("userId") userId: Long? = null
    ): Response<ApiResponse<Map<String, Int>>>

    /** 我关注的人（分页） */
    @GET("api/v1/follow/following")
    suspend fun getFollowingList(
        @Query("userId") userId: Long? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<TableDataInfo<Map<String, Any>>>

    /** 我的粉丝（分页） */
    @GET("api/v1/follow/fans")
    suspend fun getFansList(
        @Query("userId") userId: Long? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<TableDataInfo<Map<String, Any>>>
}
