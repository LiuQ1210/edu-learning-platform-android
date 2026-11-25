package com.github.learningplatform.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 阿星智能体接口（对齐后端 ApiV1AgentController，前缀 `/api/v1/agent`）。
 *
 * 会话、消息、任务都由 Java 侧落库，Python 只负责计算；所以这里除了问答，
 * 还要拉会话历史和轮询异步任务。
 */
interface AgentApi {

    /** 课程问答：`{sessionId?, question, courseId?}` → `{sessionId, messageId, answer, refs, retrieved}` */
    @POST("api/v1/agent/chat")
    suspend fun chat(@Body body: JsonObject): Response<JsonObject>

    /** 社区问答（引用帖子）：`{sessionId?, question}` → 同上，refs 是帖子而不是章节 */
    @POST("api/v1/agent/community/ask")
    suspend fun communityAsk(@Body body: JsonObject): Response<JsonObject>

    /** 视频总结（异步）：`{videoId}` → `{taskId, status}` */
    @POST("api/v1/agent/video/summary")
    suspend fun videoSummary(@Body body: JsonObject): Response<JsonObject>

    /** PPT 生成（异步）：`{topic, pages?}` → `{taskId, status}` */
    @POST("api/v1/agent/ppt/generate")
    suspend fun pptGenerate(@Body body: JsonObject): Response<JsonObject>

    /** 异步任务状态：`{task:{taskId, status, progress, outputJson, errorMsg, ...}}` */
    @GET("api/v1/agent/tasks/{taskId}")
    suspend fun task(@Path("taskId") taskId: Long): Response<JsonObject>

    /** 我的会话列表：`{total, rows:[AgentSession]}` */
    @GET("api/v1/agent/sessions")
    suspend fun sessions(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<JsonObject>

    /** 会话消息：`{rows:[AgentMessage]}` */
    @GET("api/v1/agent/sessions/{sessionId}/messages")
    suspend fun messages(@Path("sessionId") sessionId: Long): Response<JsonObject>
}
