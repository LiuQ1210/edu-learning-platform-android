package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.core.network.AppJson
import com.github.learningplatform.core.network.bool
import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.listOf
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.lookup
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.AgentApi
import com.github.learningplatform.data.remote.AgentStreamClient
import com.github.learningplatform.data.remote.AgentStreamEvent
import com.github.learningplatform.data.remote.dto.AgentActDetail
import com.github.learningplatform.data.remote.dto.AgentActOutput
import com.github.learningplatform.data.remote.dto.AgentChatResult
import com.github.learningplatform.data.remote.dto.AgentMemoryCandidate
import com.github.learningplatform.data.remote.dto.AgentMemoryDto
import com.github.learningplatform.data.remote.dto.AgentMessageDto
import com.github.learningplatform.data.remote.dto.AgentPptOutlineDto
import com.github.learningplatform.data.remote.dto.AgentPptResultDto
import com.github.learningplatform.data.remote.dto.AgentPptSectionDto
import com.github.learningplatform.data.remote.dto.AgentPptSlideDto
import com.github.learningplatform.data.remote.dto.AgentRef
import com.github.learningplatform.data.remote.dto.AgentSessionDto
import com.github.learningplatform.data.remote.dto.AgentSessionPage
import com.github.learningplatform.data.remote.dto.AgentSqlResult
import com.github.learningplatform.data.remote.dto.AgentSubtitleDto
import com.github.learningplatform.data.remote.dto.AgentTaskDto
import com.github.learningplatform.data.remote.dto.AgentTaskStart
import com.github.learningplatform.data.remote.dto.AgentTaskStepDto
import com.github.learningplatform.data.remote.dto.AgentTaskSteps
import com.github.learningplatform.data.remote.dto.AgentVideoSummaryDto
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * 阿星智能体仓库。
 *
 * 后端把「会话 / 消息 / 任务 / 记忆 / 步骤」落库，Python 只做计算，因此这里
 * 除了问答入口，还负责：
 *  - 异步任务轮询（video summary / subtitle / ppt / act）；
 *  - 执行步骤与取消；
 *  - 会话与长期记忆的增删改查；
 *  - 把 `outputJson` / `resultJson` / `refsJson` 这些**字符串**二次解析成 UI 能直接用的对象。
 */
@Singleton
class AgentRepository @Inject constructor(
    private val agentApi: AgentApi,
    private val streamClient: AgentStreamClient
) {

    // ------------------------------------------------------------ 问答

    suspend fun chat(
        question: String,
        courseId: Long?,
        sessionId: Long?,
        imageUrls: List<String> = emptyList()
    ): AgentChatResult {
        val body = buildJsonObject {
            if (question.isNotBlank()) put("question", question)
            if (courseId != null && courseId > 0) put("courseId", courseId)
            if (sessionId != null && sessionId > 0) put("sessionId", sessionId)
            if (imageUrls.isNotEmpty()) {
                put("imageUrls", JsonArray(imageUrls.map { JsonPrimitive(it) }))
            }
        }
        return agentApi.chat(body).okBody().toChatResult()
    }

    /** 一次性问答：不建会话、不落消息（纯代理，含长期记忆注入） */
    suspend fun chatAsk(
        question: String,
        courseId: Long? = null,
        imageUrls: List<String> = emptyList()
    ): AgentChatResult {
        val body = buildJsonObject {
            put("question", question)
            if (courseId != null && courseId > 0) put("courseId", courseId)
            if (imageUrls.isNotEmpty()) {
                put("imageUrls", JsonArray(imageUrls.map { JsonPrimitive(it) }))
            }
        }
        return agentApi.chatAsk(body).okBody().toChatResult()
    }

    /** 流式问答提交：返回 requestId 与 sessionId，内容走 WebSocket */
    suspend fun startStream(
        question: String,
        courseId: Long?,
        sessionId: Long?
    ): AgentChatResult {
        val body = buildJsonObject {
            put("question", question)
            if (courseId != null && courseId > 0) put("courseId", courseId)
            if (sessionId != null && sessionId > 0) put("sessionId", sessionId)
        }
        val p = agentApi.chatStream(body).okBody().payload()
        return AgentChatResult(
            requestId = p.str("requestId"),
            sessionId = p.long("sessionId")
        )
    }

    suspend fun communityAsk(question: String, sessionId: Long?): AgentChatResult {
        val body = buildJsonObject {
            put("question", question)
            if (sessionId != null && sessionId > 0) put("sessionId", sessionId)
        }
        return agentApi.communityAsk(body).okBody().toChatResult()
    }

    /**
     * 流式课程问答。
     *
     * 先建 WebSocket 并订阅 `user:{userId}`，再 POST `/chat/stream` 拿 requestId，
     * 然后按 `agent.delta` 增量回调，`agent.done` 收尾。
     *
     * WebSocket 连不上（未登录、被网关拦截、服务端未开）时**回退到同步 `/chat`**：
     * 此时还没有落用户消息，回退不会产生重复。
     */
    suspend fun chatStreaming(
        question: String,
        courseId: Long?,
        sessionId: Long?,
        onDelta: (String) -> Unit = {}
    ): AgentChatResult {
        val stream = runCatching { streamClient.open() }.getOrNull()
            ?: return chat(question, courseId, sessionId)
        try {
            val start = startStream(question, courseId, sessionId)
            if (start.requestId.isBlank()) {
                throw ApiException(-1, "流式问答初始化失败，请重试")
            }
            val text = StringBuilder()
            val terminal = withTimeoutOrNull(STREAM_TIMEOUT_MILLIS) {
                stream.flow(start.requestId)
                    .onEach { event ->
                        if (event is AgentStreamEvent.Delta) {
                            text.append(event.content)
                            onDelta(text.toString())
                        }
                    }
                    .first { it is AgentStreamEvent.Done || it is AgentStreamEvent.Error }
            }
            if (terminal == null) {
                throw ApiException(-1, "流式回答超时，请重试")
            }
            when (terminal) {
                is AgentStreamEvent.Done -> return AgentChatResult(
                    sessionId = terminal.sessionId.takeIf { it > 0 } ?: start.sessionId,
                    messageId = terminal.messageId,
                    answer = cleanAnswer(terminal.answer.ifBlank { text.toString() }),
                    refs = terminal.refs
                )
                is AgentStreamEvent.Error -> throw ApiException(-1, terminal.message)
                else -> throw ApiException(-1, "流式回答失败，请重试")
            }
        } finally {
            stream.close()
        }
    }

    /** SQL 问答：落一条 table 消息，返回结果集与一句话结论 */
    suspend fun sqlAsk(question: String, sessionId: Long?): AgentSqlResult {
        val body = buildJsonObject {
            put("question", question)
            if (sessionId != null && sessionId > 0) put("sessionId", sessionId)
        }
        val p = agentApi.sqlAsk(body).okBody().payload()
        val (headers, rows) = tableOf(p.lookup("rows"))
        return AgentSqlResult(
            answer = cleanAnswer(p.str("answer")),
            sql = p.str("sql"),
            headers = headers,
            rows = rows,
            rowCount = p.int("rowCount"),
            error = p.bool("error"),
            sessionId = p.long("sessionId"),
            messageId = p.long("messageId")
        )
    }

    // ------------------------------------------------------------ 异步任务

    /** 提交视频总结，返回任务与落库的会话/消息 ID */
    suspend fun startVideoSummary(videoId: Long, sessionId: Long? = null): AgentTaskStart =
        parseTaskStart(agentApi.videoSummary(buildJsonObject {
            put("videoId", videoId)
            if (sessionId != null && sessionId > 0) put("sessionId", sessionId)
        }).okBody().payload())

    /** 提交视频字幕（只跑 ASR，不调用大模型）；force=true 强制重转 */
    suspend fun startVideoSubtitle(
        videoId: Long,
        sessionId: Long? = null,
        force: Boolean = false
    ): AgentTaskStart =
        parseTaskStart(agentApi.videoSubtitle(buildJsonObject {
            put("videoId", videoId)
            if (sessionId != null && sessionId > 0) put("sessionId", sessionId)
            if (force) put("force", true)
        }).okBody().payload())

    /**
     * 提交 PPT 生成。
     *
     * 契约：topic 只做标题，最长 100 字；content 是正文素材，最长 8000 字。
     * 本地先校验，避免白跑一次网络再拿回中文错误。
     */
    suspend fun startPpt(
        topic: String,
        content: String = "",
        videoId: Long? = null,
        sessionId: Long? = null,
        pages: Int = 6
    ): AgentTaskStart {
        val trimmedTopic = topic.trim()
        if (trimmedTopic.isEmpty()) throw ApiException(400, "PPT 主题不能为空")
        if (trimmedTopic.length > 100) {
            throw ApiException(
                400,
                "PPT 主题最长 100 字（当前 ${trimmedTopic.length} 字）；" +
                    "正文素材请放到素材里，标题只做标题"
            )
        }
        if (content.length > 8000) {
            throw ApiException(400, "PPT 素材最长 8000 字（当前 ${content.length} 字），请精简后再提交")
        }
        val body = buildJsonObject {
            put("topic", trimmedTopic)
            put("pages", pages)
            if (content.isNotBlank()) put("content", content)
            if (videoId != null && videoId > 0) put("videoId", videoId)
            if (sessionId != null && sessionId > 0) put("sessionId", sessionId)
        }
        return parseTaskStart(agentApi.pptGenerate(body).okBody().payload())
    }

    /** 提交执行型智能体目标（异步）：`{goal, sessionId?, maxSteps?, idempotencyKey?}` */
    suspend fun act(
        goal: String,
        sessionId: Long? = null,
        maxSteps: Int = 8,
        idempotencyKey: String = ""
    ): AgentTaskStart {
        val trimmed = goal.trim()
        if (trimmed.isEmpty()) throw ApiException(400, "目标不能为空")
        if (trimmed.length > 500) {
            throw ApiException(400, "目标最长 500 字（当前 ${trimmed.length} 字）")
        }
        val body = buildJsonObject {
            put("goal", trimmed)
            put("maxSteps", maxSteps.coerceIn(1, 10))
            if (sessionId != null && sessionId > 0) put("sessionId", sessionId)
            if (idempotencyKey.isNotBlank()) put("idempotencyKey", idempotencyKey)
        }
        return parseTaskStart(agentApi.act(body).okBody().payload())
    }

    suspend fun task(taskId: Long): AgentTaskDto {
        val p = agentApi.task(taskId).okBody().payload()
        val t = (p.lookup("task") as? JsonObject) ?: p
        return taskOf(t, taskId)
    }

    /** 任务步骤（RUNNING 期间也能拿到已完成步骤） */
    suspend fun taskSteps(taskId: Long): AgentTaskSteps {
        val p = agentApi.taskSteps(taskId).okBody().payload()
        return AgentTaskSteps(
            taskId = p.long("taskId", taskId),
            status = p.str("status", AgentTaskDto.STATUS_PENDING),
            progress = p.int("progress"),
            currentStep = p.str("currentStep"),
            steps = stepsOf(p.lookup("steps"))
        )
    }

    /** 取消任务：已结束的任务返回 false，不报错 */
    suspend fun cancelTask(taskId: Long): Boolean {
        val p = agentApi.cancelTask(taskId).okBody().payload()
        return p.bool("canceled")
    }

    /**
     * 轮询到任务结束。
     *
     * 视频要跑 ASR、PPT 要调大模型，耗时按分钟计，不能用 Retrofit 的普通超时去等，
     * 所以提交接口立即返回 taskId，这里按 [pollMillis] 轮询。
     */
    suspend fun awaitTask(
        taskId: Long,
        pollMillis: Long = 1500,
        timeoutMillis: Long = 300_000,
        onProgress: (AgentTaskDto) -> Unit = {}
    ): AgentTaskDto {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (true) {
            val current = task(taskId)
            onProgress(current)
            if (current.isDone) return current
            if (System.currentTimeMillis() >= deadline) {
                throw ApiException(-1, "生成超时，请稍后重试")
            }
            delay(pollMillis)
        }
    }

    /** 执行型智能体详情：一次拿到任务、步骤、currentStep、answer、refs */
    suspend fun actDetail(taskId: Long): AgentActDetail {
        val p = agentApi.actDetail(taskId).okBody().payload()
        val task = (p.lookup("task") as? JsonObject)?.let { taskOf(it, taskId) }
        return AgentActDetail(
            task = task,
            status = p.str("status", task?.status ?: AgentTaskDto.STATUS_PENDING),
            progress = p.int("progress", task?.progress ?: 0),
            currentStep = p.str("currentStep"),
            steps = stepsOf(p.lookup("steps")),
            answer = cleanAnswer(p.str("answer")),
            refs = refsOf(p.lookup("refs")),
            rounds = p.int("rounds"),
            maxSteps = p.int("maxSteps"),
            toolCallCount = p.int("toolCallCount"),
            truncated = p.bool("truncated"),
            degraded = p.bool("degraded"),
            degradeReason = p.str("degradeReason"),
            errorCode = p.str("errorCode"),
            errorMsg = task?.errorMsg.orEmpty()
        )
    }

    /**
     * 轮询 act 任务：每 2 秒拉一次步骤，RUNNING 期间就能把已完成步骤展示出来。
     * 结束时返回最后一次详情（包含 answer / refs / 降级说明）。
     */
    suspend fun awaitAct(
        taskId: Long,
        pollMillis: Long = 2000,
        timeoutMillis: Long = 320_000,
        onUpdate: (AgentActDetail) -> Unit = {}
    ): AgentActDetail {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (true) {
            val detail = actDetail(taskId)
            onUpdate(detail)
            if (detail.isDone) return detail
            if (System.currentTimeMillis() >= deadline) {
                throw ApiException(-1, "任务执行超时，可在步骤面板查看已完成部分")
            }
            delay(pollMillis)
        }
    }

    // ------------------------------------------------------------ 会话

    suspend fun sessions(pageNum: Int = 1, pageSize: Int = 20): AgentSessionPage {
        val p = agentApi.sessions(pageNum, pageSize).okBody().payload()
        return AgentSessionPage(
            total = p.int("total"),
            items = p.listOf("rows").mapNotNull { it as? JsonObject }.map { row ->
                sessionOf(row)
            }
        )
    }

    suspend fun createSession(title: String): AgentSessionDto {
        val body = buildJsonObject { if (title.isNotBlank()) put("title", title) }
        val p = agentApi.createSession(body).okBody().payload()
        return (p.lookup("session") as? JsonObject)?.let { sessionOf(it) }
            ?: AgentSessionDto(title = title.ifBlank { "新会话" })
    }

    suspend fun renameSession(sessionId: Long, title: String): AgentSessionDto? {
        val p = agentApi.renameSession(sessionId, buildJsonObject { put("title", title) })
            .okBody().payload()
        return (p.lookup("session") as? JsonObject)?.let { sessionOf(it) }
    }

    suspend fun deleteSession(sessionId: Long) {
        agentApi.deleteSession(sessionId).okBody()
    }

    /**
     * 会话消息。
     *
     * @param beforeMessageId 游标：拉更早的一页；null 表示最近一页（默认 30 条）
     */
    suspend fun messages(sessionId: Long, beforeMessageId: Long? = null): List<AgentMessageDto> {
        val p = agentApi.messages(sessionId, beforeMessageId).okBody().payload()
        return p.listOf("rows").mapNotNull { it as? JsonObject }
            .filter { it.str("role") == "user" || it.str("role") == "assistant" }
            .map { row ->
                val (refs, images) = refsAndImages(row.lookup("refsJson"))
                val task = (row.lookup("task") as? JsonObject)?.let {
                    taskOf(it, row.long("taskId"))
                }
                AgentMessageDto(
                    messageId = row.long("messageId"),
                    role = row.str("role"),
                    content = cleanAnswer(row.str("content")),
                    contentType = row.str("contentType", "text"),
                    refs = refs,
                    images = images,
                    taskId = row.long("taskId"),
                    task = task,
                    resultJson = row.str("resultJson"),
                    createTime = row.dateText("createTime")
                )
            }
    }

    // ------------------------------------------------------------ 长期记忆

    suspend fun memories(): List<AgentMemoryDto> {
        val p = agentApi.memories().okBody().payload()
        return p.listOf("rows").mapNotNull { it as? JsonObject }.map { row ->
            AgentMemoryDto(
                memoryId = row.long("memoryId"),
                content = row.str("content"),
                source = row.str("source", "manual"),
                status = row.str("status"),
                createTime = row.dateText("createTime"),
                updateTime = row.dateText("updateTime")
            )
        }
    }

    /** 新增记忆；后端按内容幂等，重复保存不会叠加 */
    suspend fun addMemory(content: String, source: String = "app"): Long {
        val body = buildJsonObject {
            put("content", content.trim())
            put("source", source)
        }
        return agentApi.addMemory(body).okBody().payload().long("memoryId")
    }

    suspend fun updateMemory(memoryId: Long, content: String) {
        agentApi.updateMemory(memoryId, buildJsonObject { put("content", content.trim()) }).okBody()
    }

    suspend fun deleteMemory(memoryId: Long) {
        agentApi.deleteMemory(memoryId).okBody()
    }

    // ------------------------------------------------------------ 图片

    /**
     * 上传聊天图片。
     *
     * 后端限制：单张 ≤ 5MB，只认 JPEG/PNG/GIF/WebP（按文件头判断），
     * 地址必须上传后才能被 /agent/chat 引用。
     */
    suspend fun uploadChatImage(file: File): String {
        require(file.exists() && file.length() > 0) { "图片不存在或内容为空" }
        if (file.length() > 5L * 1024 * 1024) throw ApiException(400, "图片不能超过 5MB")
        val mime = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        val part = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody(mime.toMediaTypeOrNull())
        )
        return agentApi.uploadChatImage(part).okBody().payload().str("url")
    }

    // ------------------------------------------------------------ 解析

    private fun parseTaskStart(p: JsonObject): AgentTaskStart = AgentTaskStart(
        taskId = p.long("taskId"),
        status = p.str("status", AgentTaskDto.STATUS_PENDING),
        sessionId = p.long("sessionId"),
        messageId = p.long("messageId"),
        idempotent = p.bool("idempotent")
    )

    private fun sessionOf(row: JsonObject): AgentSessionDto = AgentSessionDto(
        sessionId = row.long("sessionId"),
        title = row.str("title"),
        scene = row.str("scene", "chat"),
        lastMessageTime = row.dateText("lastMessageTime"),
        createTime = row.dateText("createTime")
    )

    private fun taskOf(t: JsonObject, fallbackId: Long): AgentTaskDto = AgentTaskDto(
        taskId = t.long("taskId", fallbackId),
        taskType = t.str("taskType"),
        status = t.str("status", AgentTaskDto.STATUS_PENDING),
        progress = t.int("progress"),
        inputJson = t.str("inputJson"),
        outputJson = t.str("outputJson"),
        errorMsg = t.str("errorMsg")
    )

    private fun stepsOf(element: JsonElement?): List<AgentTaskStepDto> =
        (element as? JsonArray)?.mapNotNull { it as? JsonObject }?.map { row ->
            AgentTaskStepDto(
                seq = row.int("seq"),
                stepType = row.str("stepType"),
                toolName = row.str("toolName"),
                argsJson = row.str("argsJson"),
                observationJson = row.str("observationJson"),
                status = row.str("status"),
                errorCode = row.str("errorCode"),
                elapsedMs = (row["elapsedMs"] as? JsonPrimitive)?.longOrNull?.toInt()
            )
        }.orEmpty()

    private fun JsonObject.toChatResult(): AgentChatResult {
        val p = payload()
        return AgentChatResult(
            sessionId = p.long("sessionId"),
            messageId = p.long("messageId"),
            answer = cleanAnswer(p.str("answer")),
            refs = refsOf(p.lookup("refs")),
            retrieved = p.int("retrieved"),
            memoryCandidates = memoryCandidatesOf(p.lookup("memoryCandidates"))
        )
    }

    private fun memoryCandidatesOf(element: JsonElement?): List<AgentMemoryCandidate> =
        (element as? JsonArray)?.mapNotNull { it as? JsonObject }?.map { item ->
            AgentMemoryCandidate(
                candidateId = item.str("candidateId"),
                content = item.str("content"),
                reason = item.str("reason")
            )
        }.orEmpty()

    private fun JsonObject.dateText(key: String): String {
        val value = this[key] as? JsonPrimitive ?: return ""
        if (value.isString) return value.content
        val millis = value.longOrNull ?: return value.content
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date(millis))
    }

    companion object {

        /** 流式回答的最长等待：大模型首字 + 全文一般几十秒，留足余量 */
        private const val STREAM_TIMEOUT_MILLIS = 180_000L

        /** 旧版回答结尾带 `[来源: 课程-章节]` 标记，refs 已单独展示，正文里去掉避免重复 */
        private val SOURCE_TAG = Regex("""\[来源[:：][^\]]*]""")

        fun cleanAnswer(text: String): String = text.replace(SOURCE_TAG, "").trim()

        /** refs 可能是数组，也可能是历史消息里的 JSON 字符串（refsJson） */
        private fun refsOf(element: JsonElement?): List<AgentRef> {
            val array = jsonArrayOf(element) ?: return emptyList()
            return array.mapNotNull { it as? JsonObject }.map { item -> refOf(item) }
        }

        private fun refOf(item: JsonObject): AgentRef = AgentRef(
            course = item.str("course"),
            chapter = item.str("chapter"),
            postId = item.long("postId"),
            postTitle = item.str("postTitle"),
            category = item.str("category"),
            excerpt = item.str("excerpt"),
            title = item.str("title"),
            courseId = item.long("courseId"),
            chapterTitle = item.str("chapterTitle")
        )

        /**
         * refsJson 有两种形态：
         *  - 字符串/数组：课程或帖子引用；
         *  - `{"images":[{"url":...}]}`：图片提问的附件地址。
         */
        private fun refsAndImages(element: JsonElement?): Pair<List<AgentRef>, List<String>> {
            val parsed = when (element) {
                is JsonPrimitive -> runCatching {
                    AppJson.parseToJsonElement(element.content)
                }.getOrNull()
                else -> element
            }
            return when (parsed) {
                is JsonObject -> emptyList<AgentRef>() to imagesOf(parsed)
                is JsonArray -> parsed.mapNotNull { it as? JsonObject }.map { refOf(it) } to emptyList()
                else -> emptyList<AgentRef>() to emptyList()
            }
        }

        private fun imagesOf(obj: JsonObject): List<String> =
            (obj["images"] as? JsonArray)
                ?.mapNotNull { it as? JsonObject }
                ?.mapNotNull { it.str("url").takeIf { url -> url.isNotBlank() } }
                .orEmpty()

        private fun jsonArrayOf(element: JsonElement?): JsonArray? = when (element) {
            is JsonArray -> element
            is JsonPrimitive -> runCatching {
                AppJson.parseToJsonElement(element.content) as? JsonArray
            }.getOrNull()
            else -> null
        }

        /** 视频总结任务的 outputJson */
        fun parseVideoSummary(outputJson: String): AgentVideoSummaryDto? {
            val obj = parseObject(outputJson) ?: return null
            return AgentVideoSummaryDto(
                videoId = obj.long("videoId"),
                videoTitle = obj.str("videoTitle"),
                courseId = obj.long("courseId"),
                durationMinutes = obj.numberInt("durationMinutes"),
                summary = obj.str("summary"),
                transcriptChars = obj.int("transcriptChars"),
                asr = obj.bool("asr"),
                fallback = obj.bool("fallback"),
                asrErrorCode = obj.str("asrErrorCode"),
                subtitleUrl = obj.str("subtitleUrl"),
                subtitleVttUrl = obj.str("subtitleVttUrl"),
                subtitleFormat = obj.str("subtitleFormat"),
                subtitleSegments = obj.int("subtitleSegments")
            )
        }

        /** 视频字幕任务的 outputJson */
        fun parseSubtitle(outputJson: String): AgentSubtitleDto? {
            val obj = parseObject(outputJson) ?: return null
            return AgentSubtitleDto(
                videoId = obj.long("videoId"),
                videoTitle = obj.str("videoTitle"),
                courseId = obj.long("courseId"),
                durationMinutes = obj.numberInt("durationMinutes"),
                subtitleUrl = obj.str("subtitleUrl"),
                subtitleVttUrl = obj.str("subtitleVttUrl"),
                subtitleFormat = obj.str("subtitleFormat", "srt"),
                subtitleSegments = obj.int("subtitleSegments"),
                transcriptChars = obj.int("transcriptChars"),
                cached = obj.bool("cached"),
                asr = obj.bool("asr")
            )
        }

        /**
         * PPT 任务的 outputJson：url/fileName 用于下载，outline/slides 用于应用内演示。
         *
         * 旧任务 `slides` 是数字（页数）且没有 pageCount，这里按老结构降级，只展示 outline。
         */
        fun parsePpt(outputJson: String): AgentPptResultDto? {
            val obj = parseObject(outputJson) ?: return null
            val outlineObj = obj["outline"] as? JsonObject
            val outline = if (outlineObj == null) {
                AgentPptOutlineDto(title = obj.str("title"))
            } else {
                AgentPptOutlineDto(
                    title = outlineObj.str("title"),
                    subtitle = outlineObj.str("subtitle"),
                    sections = (outlineObj["sections"] as? JsonArray)
                        ?.mapNotNull { it as? JsonObject }
                        ?.map { AgentPptSectionDto(it.str("title"), it.str("summary")) }
                        .orEmpty(),
                    highlights = outlineObj.strList("highlights"),
                    keyPoints = outlineObj.strList("keyPoints"),
                    closing = outlineObj.str("closing")
                )
            }
            val slidesElement = obj["slides"]
            val slides = if (slidesElement is JsonArray) {
                slidesElement.mapNotNull { it as? JsonObject }.map { slide ->
                    AgentPptSlideDto(
                        page = slide.int("page"),
                        type = slide.str("type"),
                        title = slide.str("title"),
                        bullets = slide.strList("bullets")
                    )
                }
            } else {
                emptyList()
            }
            val pageCount = obj.int("pageCount").takeIf { it > 0 }
                ?: (slidesElement as? JsonPrimitive)?.longOrNull?.toInt()
                ?: slides.size
            return AgentPptResultDto(
                url = obj.str("url"),
                fileName = obj.str("fileName"),
                pageCount = pageCount,
                slides = slides,
                outline = outline,
                videoId = obj.long("videoId")
            )
        }

        /** 执行型智能体的 outputJson：answer / refs / 轮次 / 截断说明 */
        fun parseActOutput(outputJson: String): AgentActOutput? {
            val obj = parseObject(outputJson) ?: return null
            return AgentActOutput(
                answer = cleanAnswer(obj.str("answer")),
                refs = refsOf(obj["refs"]),
                rounds = obj.int("rounds"),
                maxSteps = obj.int("maxSteps"),
                toolCallCount = obj.int("toolCallCount"),
                truncated = obj.bool("truncated"),
                degraded = obj.bool("degraded"),
                degradeReason = obj.str("degradeReason"),
                errorCode = obj.str("errorCode"),
                errorMsg = obj.str("errorMessage")
            )
        }

        /** SQL 历史消息的 resultJson 快照：`{sql, rows, rowCount, error}` */
        fun parseSqlSnapshot(resultJson: String): AgentSqlResult? {
            val obj = parseObject(resultJson) ?: return null
            val (headers, rows) = tableOf(obj["rows"])
            return AgentSqlResult(
                sql = obj.str("sql"),
                headers = headers,
                rows = rows,
                rowCount = obj.int("rowCount"),
                error = obj.bool("error")
            )
        }

        /** 表格：行是 JSON 对象，列顺序按首次出现顺序合并 */
        private fun tableOf(element: JsonElement?): Pair<List<String>, List<List<String>>> {
            val objects = (element as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
            if (objects.isEmpty()) return emptyList<String>() to emptyList()
            val headers = LinkedHashSet<String>()
            objects.forEach { row -> row.keys.forEach { headers.add(it) } }
            val columns = headers.toList()
            val rows = objects.map { row -> columns.map { key -> row[key]?.cellText().orEmpty() } }
            return columns to rows
        }

        private fun JsonElement.cellText(): String = when (this) {
            is JsonNull -> ""
            is JsonPrimitive -> content
            is JsonObject -> toString()
            is JsonArray -> joinToString("，") { it.cellText() }
        }

        private fun parseObject(raw: String): JsonObject? =
            if (raw.isBlank()) null
            else runCatching { AppJson.parseToJsonElement(raw) as? JsonObject }.getOrNull()

        private fun JsonObject.strList(key: String): List<String> =
            (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()

        /** 后端 durationMinutes 是 float，intOrNull 会取不到，这里退回 double */
        private fun JsonObject.numberInt(key: String): Int {
            val value = this[key] as? JsonPrimitive ?: return 0
            return value.longOrNull?.toInt() ?: value.doubleOrNullDefault()
        }

        private fun JsonPrimitive.doubleOrNullDefault(): Int {
            val text = content
            return text.toDoubleOrNull()?.toInt() ?: 0
        }
    }
}
