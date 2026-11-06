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

    /** 新增评分（content 可选，≤500字，不传=只打星） */
    @POST("api/v1/rating/add")
    suspend fun addRating(
        @Query("courseId") courseId: Long,
        @Query("score") score: Int,
        @Query("content") content: String? = null
    ): Response<JsonObject>

    /** 修改评分（content 可选，传空串=清空文字） */
    @POST("api/v1/rating/update")
    suspend fun updateRating(
        @Query("courseId") courseId: Long,
        @Query("score") score: Int,
        @Query("content") content: String? = null
    ): Response<JsonObject>

    /** 查询评分：我的评分 + 课程平均分 + 评分人数 */
    @GET("api/v1/rating/info")
    suspend fun getRatingInfo(
        @Query("courseId") courseId: Long
    ): Response<JsonObject>

    /** 评分列表（分页） */
    @GET("api/v1/rating/list")
    suspend fun getRatingList(
        @Query("courseId") courseId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<JsonObject>
}
