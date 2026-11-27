package com.github.learningplatform.data.remote

import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 阿星智能体接口（对齐后端 ApiV1AgentController / ApiV1AgentActController，
 * 前缀 `/api/v1/agent`）。
 *
 * 会话、消息、任务、记忆、步骤都由 Java 侧落库，Python 只负责计算；
 * 所以这里除了问答，还要拉会话历史、轮询异步任务与执行步骤。
 */
interface AgentApi {

    // ------------------------------------------------------------ 问答

    /** 课程问答（落会话）：`{question, courseId?, sessionId?, imageUrls?}` */
    @POST("api/v1/agent/chat")
    suspend fun chat(@Body body: JsonObject): Response<JsonObject>

    /** 课程问答（纯代理、不落库） */
    @POST("api/v1/agent/chat/ask")
    suspend fun chatAsk(@Body body: JsonObject): Response<JsonObject>

    /** 课程问答流式：立即返回 `{requestId, sessionId}`，内容走 WebSocket */
    @POST("api/v1/agent/chat/stream")
    suspend fun chatStream(@Body body: JsonObject): Response<JsonObject>

    /** 社区问答（引用帖子）：`{sessionId?, question}` */
    @POST("api/v1/agent/community/ask")
    suspend fun communityAsk(@Body body: JsonObject): Response<JsonObject>

    /** 自然语言转 SQL：`{question, sessionId?}` */
    @POST("api/v1/agent/sql/ask")
    suspend fun sqlAsk(@Body body: JsonObject): Response<JsonObject>

    // ------------------------------------------------------------ 异步任务

    /** 视频总结（异步）：`{videoId, sessionId?}` → `{taskId, status, sessionId, messageId}` */
    @POST("api/v1/agent/video/summary")
    suspend fun videoSummary(@Body body: JsonObject): Response<JsonObject>

    /** 视频字幕（异步）：`{videoId, sessionId?, force?}` → 同上 */
    @POST("api/v1/agent/video/subtitle")
    suspend fun videoSubtitle(@Body body: JsonObject): Response<JsonObject>

    /** PPT 生成（异步）：`{topic, content?, pages?, videoId?, sessionId?}` → 同上 */
    @POST("api/v1/agent/ppt/generate")
    suspend fun pptGenerate(@Body body: JsonObject): Response<JsonObject>

    /** 异步任务状态：`{task:{taskId, status, progress, outputJson, errorMsg, ...}}` */
    @GET("api/v1/agent/tasks/{taskId}")
    suspend fun task(@Path("taskId") taskId: Long): Response<JsonObject>

    /** 任务执行步骤：`{taskId, status, progress, currentStep, steps}` */
    @GET("api/v1/agent/tasks/{taskId}/steps")
    suspend fun taskSteps(@Path("taskId") taskId: Long): Response<JsonObject>

    /** 取消任务（长任务「停止」按钮）：`{taskId, status, canceled}` */
    @POST("api/v1/agent/tasks/{taskId}/cancel")
    suspend fun cancelTask(@Path("taskId") taskId: Long): Response<JsonObject>

    // ------------------------------------------------------------ 执行型智能体

    /** 提交执行目标：`{goal, sessionId?, maxSteps?, idempotencyKey?}` → `{taskId, status, sessionId, idempotent}` */
    @POST("api/v1/agent/act")
    suspend fun act(@Body body: JsonObject): Response<JsonObject>

    /** 执行详情（含步骤、currentStep、answer、refs） */
    @GET("api/v1/agent/act/{taskId}")
    suspend fun actDetail(@Path("taskId") taskId: Long): Response<JsonObject>

    // ------------------------------------------------------------ 会话

    /** 我的会话列表（分页）：`{total, rows:[AgentSession]}` */
    @GET("api/v1/agent/sessions")
    suspend fun sessions(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<JsonObject>

    /** 新建会话：`{title?}` → `{session}` */
    @POST("api/v1/agent/sessions")
    suspend fun createSession(@Body body: JsonObject): Response<JsonObject>

    /** 会话消息（游标分页）：`{rows:[AgentMessage]}` */
    @GET("api/v1/agent/sessions/{sessionId}/messages")
    suspend fun messages(
        @Path("sessionId") sessionId: Long,
        @Query("beforeMessageId") beforeMessageId: Long? = null,
        @Query("pageSize") pageSize: Int = 30
    ): Response<JsonObject>

    /** 重命名会话：`{title}` → `{session}` */
    @PUT("api/v1/agent/sessions/{sessionId}")
    suspend fun renameSession(
        @Path("sessionId") sessionId: Long,
        @Body body: JsonObject
    ): Response<JsonObject>

    /** 软删会话（幂等） */
    @DELETE("api/v1/agent/sessions/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: Long): Response<JsonObject>

    // ------------------------------------------------------------ 长期记忆

    /** 记忆列表：`{total, rows:[AgentMemory]}` */
    @GET("api/v1/agent/memories")
    suspend fun memories(): Response<JsonObject>

    /** 新增记忆：`{content, source?}` → `{memoryId}` */
    @POST("api/v1/agent/memories")
    suspend fun addMemory(@Body body: JsonObject): Response<JsonObject>

    /** 更新记忆：`{content}` */
    @PUT("api/v1/agent/memories/{memoryId}")
    suspend fun updateMemory(
        @Path("memoryId") memoryId: Long,
        @Body body: JsonObject
    ): Response<JsonObject>

    /** 删除记忆（幂等） */
    @DELETE("api/v1/agent/memories/{memoryId}")
    suspend fun deleteMemory(@Path("memoryId") memoryId: Long): Response<JsonObject>

    // ------------------------------------------------------------ 聊天图片

    /**
     * 聊天图片上传：`multipart/form-data`，字段名 `file`。
     *
     * 注意前缀是 `/api/v1/upload`（不在 agent 下），返回 `{code, url, objectKey}`；
     * 只有 `agent/chat/` 前缀的地址才允许被 /agent/chat 引用。
     */
    @Multipart
    @POST("api/v1/upload/image")
    suspend fun uploadChatImage(@Part file: MultipartBody.Part): Response<JsonObject>
}
