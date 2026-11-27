package com.github.learningplatform.data.remote

import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.core.network.AppJson
import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.lookup
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.local.UserPreferences
import com.github.learningplatform.data.remote.dto.AgentRef
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * 智能体流式问答的 WebSocket 客户端。
 *
 * 后端端点：`ws://<host>/ws/forum?token=<API_TOKEN>`，连接后先订阅 `user:{userId}`，
 * 然后 `/agent/chat/stream` 的增量以 `agent.start / agent.delta / agent.done / agent.error`
 * 事件推送过来。事件不落库、不补发，所以只在本次请求期间保持连接。
 */
@Singleton
class AgentStreamClient @Inject constructor(
    private val userPreferences: UserPreferences
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        // 流式回答期间连接可能长时间没有数据，读超时交给业务层控制
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    /**
     * 建立连接并订阅自己的 topic。
     *
     * @return 连上返回会话；未登录 / 连接失败返回 null，调用方回退到同步 `/chat`
     */
    suspend fun open(): AgentStreamSession? {
        val token = runCatching { userPreferences.getToken() }.getOrNull().orEmpty()
        if (token.isBlank()) return null
        val userId = runCatching { userPreferences.currentUserId() }.getOrNull().orEmpty()
        if (userId.isBlank()) return null
        return runCatching {
            val session = AgentStreamSession(client, wsUrl(token), "user:$userId")
            session.start()
            if (session.awaitOpen()) {
                session
            } else {
                session.close()
                null
            }
        }.getOrNull()
    }

    private fun wsUrl(token: String): String {
        val base = Constants.BASE_URL
        val scheme = when {
            base.startsWith("https://", ignoreCase = true) -> "wss://"
            base.startsWith("http://", ignoreCase = true) -> "ws://"
            else -> "ws://"
        }
        val host = base.substringAfter("://").removeSuffix("/")
        val encoded = URLEncoder.encode(token, "UTF-8")
        return "$scheme$host/ws/forum?token=$encoded"
    }
}

/** 一次流式问答的 WebSocket 会话；用完必须 [close] */
class AgentStreamSession(
    private val client: OkHttpClient,
    private val url: String,
    private val topic: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val opened = CompletableDeferred<Boolean>()

    private val events = MutableSharedFlow<AgentStreamEvent>(
        replay = 0,
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var socket: WebSocket? = null

    fun start() {
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // 必须先订阅自己的 topic，否则服务端不会推送
                webSocket.send("""{"action":"subscribe","topics":["$topic"]}""")
                opened.complete(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseEvent(text)?.let { event -> scope.launch { events.emit(event) } }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                opened.complete(false)
                scope.launch {
                    events.emit(AgentStreamEvent.Error("", t.message ?: "实时连接已断开"))
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                opened.complete(false)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
        })
    }

    suspend fun awaitOpen(timeoutMillis: Long = 8_000): Boolean =
        withTimeoutOrNull(timeoutMillis) { opened.await() } ?: false

    /** 只保留本次 requestId 的事件；无 requestId 的断开错误也放行 */
    fun flow(requestId: String): Flow<AgentStreamEvent> =
        events.filter { it.requestId == requestId || it.requestId.isEmpty() }

    fun close() {
        runCatching { socket?.close(1000, "done") }
        scope.cancel()
    }

    private fun parseEvent(text: String): AgentStreamEvent? {
        val root = runCatching { AppJson.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: return null
        val type = root.str("type")
        val data = root.lookup("data") as? JsonObject ?: root
        return when (type) {
            "agent.start" -> AgentStreamEvent.Start(
                requestId = data.str("requestId"),
                sessionId = data.long("sessionId")
            )
            "agent.delta" -> AgentStreamEvent.Delta(
                requestId = data.str("requestId"),
                seq = data.int("seq"),
                content = data.str("content")
            )
            "agent.done" -> AgentStreamEvent.Done(
                requestId = data.str("requestId"),
                sessionId = data.long("sessionId"),
                messageId = data.long("messageId"),
                answer = data.str("answer"),
                refs = refsOf(data.lookup("refs"))
            )
            "agent.error" -> AgentStreamEvent.Error(
                requestId = data.str("requestId"),
                message = data.str("message", "智能体服务暂不可用")
            )
            "pong" -> null
            else -> null
        }
    }

    private fun refsOf(element: kotlinx.serialization.json.JsonElement?): List<AgentRef> =
        (element as? kotlinx.serialization.json.JsonArray)
            ?.mapNotNull { it as? JsonObject }
            ?.map { item ->
                AgentRef(
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
            }
            .orEmpty()
}

/** 服务端推送的流式事件 */
sealed interface AgentStreamEvent {
    val requestId: String

    data class Start(
        override val requestId: String,
        val sessionId: Long
    ) : AgentStreamEvent

    data class Delta(
        override val requestId: String,
        val seq: Int = 0,
        val content: String
    ) : AgentStreamEvent

    data class Done(
        override val requestId: String,
        val sessionId: Long,
        val messageId: Long,
        val answer: String,
        val refs: List<AgentRef> = emptyList()
    ) : AgentStreamEvent

    data class Error(
        override val requestId: String,
        val message: String
    ) : AgentStreamEvent
}
