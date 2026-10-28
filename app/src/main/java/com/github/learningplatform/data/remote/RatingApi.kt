package com.github.learningplatform.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 评分模块 ApiV1 接口（对齐后端 ApiV1RatingController）。
 *
 * 后端返回平铺字段：add/update -> `{code,msg,myScore,avgScore,ratingCount}`，
 * info -> 同结构。
 */
interface RatingApi {

    /** 新增评分 */
    @POST("api/v1/rating/add")
    suspend fun addRating(
        @Query("courseId") courseId: Long,
        @Query("score") score: Int
    ): Response<JsonObject>

    /** 修改评分 */
    @POST("api/v1/rating/update")
    suspend fun updateRating(
        @Query("courseId") courseId: Long,
        @Query("score") score: Int
    ): Response<JsonObject>

    /** 查询评分：我的评分 + 课程平均分 + 评分人数 */
    @GET("api/v1/rating/info")
    suspend fun getRatingInfo(
        @Query("courseId") courseId: Long
    ): Response<JsonObject>
}
