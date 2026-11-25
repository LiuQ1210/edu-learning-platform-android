package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.AppJson
import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.core.network.bool
import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.listOf
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.lookup
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.AgentApi
import com.github.learningplatform.data.remote.dto.AgentChatResult
import com.github.learningplatform.data.remote.dto.AgentMessageDto
import com.github.learningplatform.data.remote.dto.AgentPptOutlineDto
import com.github.learningplatform.data.remote.dto.AgentPptResultDto
import com.github.learningplatform.data.remote.dto.AgentPptSectionDto
import com.github.learningplatform.data.remote.dto.AgentRef
import com.github.learningplatform.data.remote.dto.AgentSessionDto
import com.github.learningplatform.data.remote.dto.AgentTaskDto
import com.github.learningplatform.data.remote.dto.AgentVideoSummaryDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * 阿星智能体仓库。
 *
 * 后端把「会话 / 消息 / 任务」落库，Python 只做计算，因此这里除了三个问答入口，
 * 还负责异步任务的轮询，以及把 `outputJson`（字符串）解析成 UI 能直接用的对象。
 */
@Singleton
class AgentRepository @Inject constructor(
    private val agentApi: AgentApi
) {

    // ------------------------------------------------------------ 问答

    suspend fun chat(question: String, courseId: Long?, sessionId: Long?): AgentChatResult {
        val body = buildJsonObject {
            put("question", question)
            if (courseId != null && courseId > 0) put("courseId", courseId)
            if (sessionId != null && sessionId > 0) put("sessionId", sessionId)
        }
        return agentApi.chat(body).okBody().toChatResult()
    }

    suspend fun communityAsk(question: String, sessionId: Long?): AgentChatResult {
        val body = buildJsonObject {
            put("question", question)
            if (sessionId != null && sessionId > 0) put("sessionId", sessionId)
        }
        return agentApi.communityAsk(body).okBody().toChatResult()
    }

    // ------------------------------------------------------------ 异步任务

    /** 提交视频总结，返回 taskId */
    suspend fun startVideoSummary(videoId: Long): Long =
        agentApi.videoSummary(buildJsonObject { put("videoId", videoId) })
            .okBody().payload().long("taskId")

    /** 提交 PPT 生成，返回 taskId（后端默认 6 页，10~20 页由服务端截断） */
    suspend fun startPpt(topic: String, pages: Int = 6): Long =
        agentApi.pptGenerate(buildJsonObject {
            put("topic", topic)
            put("pages", pages)
        }).okBody().payload().long("taskId")

    suspend fun task(taskId: Long): AgentTaskDto {
        val p = agentApi.task(taskId).okBody().payload()
        val t = (p.lookup("task") as? JsonObject) ?: p
        return AgentTaskDto(
            taskId = t.long("taskId", taskId),
            taskType = t.str("taskType"),
            status = t.str("status", AgentTaskDto.STATUS_PENDING),
            progress = t.int("progress"),
            outputJson = t.str("outputJson"),
            errorMsg = t.str("errorMsg")
        )
    }

    /**
     * 轮询到任务结束。
     *
     * 视频要跑 ASR、PPT 要调大模型，耗时是按分钟计的，不能用 Retrofit 的普通超时去等，
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

    // ------------------------------------------------------------ 会话

    suspend fun sessions(pageNum: Int = 1, pageSize: Int = 20): List<AgentSessionDto> {
        val p = agentApi.sessions(pageNum, pageSize).okBody().payload()
        return p.listOf("rows").mapNotNull { it as? JsonObject }.map { row ->
            AgentSessionDto(
                sessionId = row.long("sessionId"),
                title = row.str("title"),
                scene = row.str("scene", "chat"),
                lastMessageTime = row.dateText("lastMessageTime"),
                createTime = row.dateText("createTime")
            )
        }
    }

    suspend fun messages(sessionId: Long): List<AgentMessageDto> {
        val p = agentApi.messages(sessionId).okBody().payload()
        return p.listOf("rows").mapNotNull { it as? JsonObject }
            .filter { it.str("role") == "user" || it.str("role") == "assistant" }
            .map { row ->
                AgentMessageDto(
                    messageId = row.long("messageId"),
                    role = row.str("role"),
                    content = cleanAnswer(row.str("content")),
                    refs = refsOf(row.lookup("refsJson")),
                    createTime = row.dateText("createTime")
                )
            }
    }

    // ------------------------------------------------------------ 解析

    private fun JsonObject.toChatResult(): AgentChatResult {
        val p = payload()
        return AgentChatResult(
            sessionId = p.long("sessionId"),
            messageId = p.long("messageId"),
            answer = cleanAnswer(p.str("answer")),
            refs = refsOf(p.lookup("refs")),
            retrieved = p.int("retrieved")
        )
    }

    private fun JsonObject.dateText(key: String): String {
        val value = this[key] as? JsonPrimitive ?: return ""
        if (value.isString) return value.content
        val millis = value.longOrNull ?: return value.content
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date(millis))
    }

    companion object {

        /** 回答结尾带 `[来源: 课程-章节]` 标记，refs 已经单独展示，正文里去掉避免重复 */
        private val SOURCE_TAG = Regex("""\[来源[:：][^\]]*]""")

        fun cleanAnswer(text: String): String =
            text.replace(SOURCE_TAG, "").trim()

        /** refs 可能是数组，也可能是历史消息里的 JSON 字符串（refsJson） */
        private fun refsOf(element: JsonElement?): List<AgentRef> {
            val array = when (element) {
                is JsonArray -> element
                is JsonPrimitive -> runCatching {
                    AppJson.parseToJsonElement(element.content) as? JsonArray
                }.getOrNull()
                else -> null
            } ?: return emptyList()

            return array.mapNotNull { it as? JsonObject }.map { item ->
                AgentRef(
                    course = item.str("course"),
                    chapter = item.str("chapter"),
                    postId = item.long("postId"),
                    postTitle = item.str("postTitle"),
                    category = item.str("category")
                )
            }
        }

        /** 视频总结任务的 outputJson */
        fun parseVideoSummary(outputJson: String): AgentVideoSummaryDto? {
            val obj = parseObject(outputJson) ?: return null
            return AgentVideoSummaryDto(
                videoId = obj.long("videoId"),
                videoTitle = obj.str("videoTitle"),
                courseId = obj.long("courseId"),
                durationMinutes = obj.int("durationMinutes"),
                summary = obj.str("summary"),
                transcriptChars = obj.int("transcriptChars"),
                asr = obj.bool("asr"),
                fallback = obj.bool("fallback")
            )
        }

        /** PPT 任务的 outputJson：url/fileName 用于下载，outline 用于应用内演示 */
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
            return AgentPptResultDto(
                url = obj.str("url"),
                fileName = obj.str("fileName"),
                outline = outline
            )
        }

        private fun parseObject(raw: String): JsonObject? =
            if (raw.isBlank()) null
            else runCatching { AppJson.parseToJsonElement(raw) as? JsonObject }.getOrNull()

        private fun JsonObject.strList(key: String): List<String> =
            (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
    }
}
