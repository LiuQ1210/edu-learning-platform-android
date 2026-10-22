package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** 评分模块 ApiV1 接口 */
interface RatingApi {

    /** 新增评分 */
    @POST("api/v1/rating/add")
    suspend fun addRating(
        @Query("courseId") courseId: Long,
        @Query("score") score: Int
    ): Response<ApiResponse<Map<String, Any>>>

    /** 修改评分 */
    @POST("api/v1/rating/update")
    suspend fun updateRating(
        @Query("courseId") courseId: Long,
        @Query("score") score: Int
    ): Response<ApiResponse<Map<String, Any>>>

    /** 查询评分：我的评分 + 课程平均分 + 评分人数 */
    @GET("api/v1/rating/info")
    suspend fun getRatingInfo(
        @Query("courseId") courseId: Long
    ): Response<ApiResponse<Map<String, Any>>>
}
