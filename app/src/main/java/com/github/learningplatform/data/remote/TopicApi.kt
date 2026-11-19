package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.RawTableData
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 话题接口（对齐后端 ApiV1TopicController）。
 *
 * 话题由管理员创建，客户端只读：
 *  - `GET /api/v1/topic/list`    话题列表（按文章数倒序，可按名称模糊搜索）
 *  - `GET /api/v1/topic/{topicId}` 话题详情（名称 + 简介 + 文章数）
 *
 * 话题下的文章列表复用 `GET /api/v1/post/list?topicId=xxx`，不在本接口内。
 */
interface TopicApi {

    /** 话题列表（分页；keyword 可选模糊搜索话题名） */
    @GET("api/v1/topic/list")
    suspend fun getTopicList(
        @Query("keyword") keyword: String? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<RawTableData>

    /** 话题详情（业务字段在 `data` 下） */
    @GET("api/v1/topic/{topicId}")
    suspend fun getTopicDetail(
        @Path("topicId") topicId: Long
    ): Response<JsonObject>
}
