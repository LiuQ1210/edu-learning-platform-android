package com.github.learningplatform.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** 通用评论接口（对齐后端 ApiV1CommentController，/api/v1/comment） */
interface CommentApi {

    @GET("api/v1/comment/list")
    suspend fun list(
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<JsonObject>

    @POST("api/v1/comment/add")
    suspend fun add(
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: Long,
        @Query("content") content: String,
        @Query("parentId") parentId: Long? = null
    ): Response<JsonObject>

    @POST("api/v1/comment/remove")
    suspend fun remove(
        @Query("commentId") commentId: Long
    ): Response<JsonObject>
}
