package com.github.learningplatform.ui.axing

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.remote.dto.AgentActDetail
import com.github.learningplatform.data.remote.dto.AgentMemoryDto
import com.github.learningplatform.data.remote.dto.AgentMessageDto
import com.github.learningplatform.data.remote.dto.AgentPptResultDto
import com.github.learningplatform.data.remote.dto.AgentRef
import com.github.learningplatform.data.remote.dto.AgentSessionDto
import com.github.learningplatform.data.remote.dto.AgentSubtitleDto
import com.github.learningplatform.data.remote.dto.AgentTaskDto
import com.github.learningplatform.data.remote.dto.AgentTaskStepDto
import com.github.learningplatform.data.repository.AgentRepository
import com.github.learningplatform.data.repository.CourseRepository
import com.github.learningplatform.data.repository.FileRepository
import com.github.learningplatform.data.repository.StudyRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** 长期记忆管理面板状态 */
data class AxingMemoryUiState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<AgentMemoryDto> = emptyList(),
    val draft: String = "",
    /** 正在编辑的记忆 ID；0 表示新增 */
    val editingId: Long = 0,
    val busy: Boolean = false
)

data class AxingUiState(
    val messages: List<AxingMessage> = emptyList(),
    val input: String = "",
    val capability: AxingCapability? = null,
    val course: AxingCourse? = null,
    val imageUri: String? = null,
    val isThinking: Boolean = false,
    val thinkingLabel: String = "阿星正在整理回答…",
    val showCoursePicker: Boolean = false,
    val courses: List<AxingCourse> = emptyList(),
    val coursesLoading: Boolean = false,
    val coursesError: String? = null,
    val showDrawer: Boolean = false,
    val sessions: List<AxingHistoryItem> = emptyList(),
    val sessionsLoading: Boolean = false,
    val sessionsError: String? = null,
    /** 后端会话总数，用于「加载更多历史」 */
    val sessionTotal: Int = 0,
    /** 后端会话 ID：第一次问答由后端创建，后续消息带上它延续上下文 */
    val sessionId: Long = 0,
    /** 当前会话场景：chat / course / community / sql / ppt / video / act */
    val sessionScene: String = "chat",
    /** 从播放页进入时带上当前小节，视频总结 / 字幕要用 */
    val videoId: Long = 0,
    val fullscreenDeck: AxingDeck? = null,
    /**
     * 流式回答开关。
     *
     * 默认关闭：同步 `/agent/chat` 会返回模型提议的 `memoryCandidates`，
     * 而 `/agent/chat/stream` 按契约不提供候选（避免标记混进增量文本）。
     */
    val streamingReply: Boolean = false,

    // ---- 流式与长任务 ----
    /** 流式问答已接收的增量文本 */
    val streamingText: String = "",
    /** 执行型智能体实时步骤（RUNNING 期间也会增长） */
    val liveSteps: List<AxingStepUi> = emptyList(),
    val liveNote: String? = null,
    /** 非 0 表示有可取消的长任务（视频总结 / 字幕 / PPT / 执行任务） */
    val activeTaskId: Long = 0,
    val cancelling: Boolean = false,

    // ---- 长期记忆 ----
    val memory: AxingMemoryUiState = AxingMemoryUiState(),
    /** candidateId -> saved / ignored */
    val candidateStates: Map<String, String> = emptyMap(),

    // ---- 历史会话操作 ----
    val sessionMenu: AxingHistoryItem? = null,
    val renameTarget: AxingHistoryItem? = null,
    val renameDraft: String = "",
    val deleteTarget: AxingHistoryItem? = null,

    // ---- 历史消息分页 ----
    val hasEarlier: Boolean = false,
    val loadingEarlier: Boolean = false,
    val firstMessageId: Long = 0,

    /** 一次性提示（Toast），消费后清空 */
    val notice: String? = null
)

@HiltViewModel
class AxingViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val courseRepository: CourseRepository,
    private val studyRepository: StudyRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AxingUiState())
    val state: StateFlow<AxingUiState> = _state.asStateFlow()

    private var replyJob: Job? = null
    private var memoryJob: Job? = null

    private fun update(block: (AxingUiState) -> AxingUiState) {
        _state.value = block(_state.value)
    }

    /**
     * @param videoId 播放页进入时传当前小节 ID；首页 / 课程详情进入传 0
     */
    fun initCourse(courseId: Long, courseTitle: String, videoId: Long = 0) {
        var next = _state.value
        if (videoId > 0 && next.videoId != videoId) next = next.copy(videoId = videoId)
        if (courseId > 0 && next.course?.id != courseId) {
            next = next.copy(
                course = AxingCourse(courseId, courseTitle.ifBlank { "当前课程" }),
                capability = AxingCapability.QA
            )
        }
        if (next != _state.value) _state.value = next
    }

    fun setInput(value: String) {
        update { it.copy(input = value) }
    }

    fun selectCapability(capability: AxingCapability) {
        update { it.copy(capability = if (it.capability == capability) null else capability) }
    }

    fun clearCapability() {
        update { it.copy(capability = null) }
    }

    fun useSuggestion(suggestion: AxingSuggestion) {
        update { it.copy(input = suggestion.text, capability = suggestion.capability) }
    }

    fun consumeNotice() {
        update { it.copy(notice = null) }
    }

    /** 切换流式回答：开启后逐字返回，但不再有记忆候选 */
    fun toggleStreamingReply() {
        val enabled = !_state.value.streamingReply
        update { it.copy(streamingReply = enabled) }
        notify(
            if (enabled) {
                "已开启流式回答（逐字返回，不再提供记忆候选）"
            } else {
                "已关闭流式回答，恢复完整回答与记忆候选"
            }
        )
    }

    private fun notify(message: String) {
        update { it.copy(notice = message) }
    }

    // ------------------------------------------------------------ 课程选择

    fun openCoursePicker() {
        update { it.copy(showCoursePicker = true) }
        if (_state.value.courses.isEmpty()) loadCourses()
    }

    fun closeCoursePicker() {
        update { it.copy(showCoursePicker = false) }
    }

    fun retryCourses() = loadCourses()

    private fun loadCourses() {
        if (_state.value.coursesLoading) return
        update { it.copy(coursesLoading = true, coursesError = null) }
        viewModelScope.launch {
            try {
                val page = courseRepository.getCourses(pageNum = 1, pageSize = 20)
                update {
                    it.copy(
                        coursesLoading = false,
                        courses = page.list.map { course ->
                            AxingCourse(course.courseId, course.courseName, course.categoryName)
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                update { it.copy(coursesLoading = false, coursesError = e.readableMessage()) }
            }
        }
    }

    fun selectCourse(course: AxingCourse) {
        update { it.copy(course = course, showCoursePicker = false) }
    }

    fun attachImage(uri: String) {
        update { it.copy(imageUri = uri) }
    }

    fun clearImage() {
        update { it.copy(imageUri = null) }
    }

    // ------------------------------------------------------------ 历史会话

    fun openDrawer() {
        update { it.copy(showDrawer = true) }
        loadSessions()
    }

    fun closeDrawer() {
        update { it.copy(showDrawer = false) }
    }

    fun retrySessions() = loadSessions()

    /** 加载更多历史会话（后端分页，20 条一页） */
    fun loadMoreSessions() {
        val current = _state.value
        if (current.sessionsLoading || current.sessions.size >= current.sessionTotal) return
        loadSessions(page = current.sessions.size / SESSION_PAGE_SIZE + 1)
    }

    private fun loadSessions(page: Int = 1) {
        if (_state.value.sessionsLoading) return
        update { it.copy(sessionsLoading = true, sessionsError = null) }
        viewModelScope.launch {
            try {
                val result = agentRepository.sessions(pageNum = page, pageSize = SESSION_PAGE_SIZE)
                val mapped = result.items.map { session -> session.toHistoryItem() }
                update {
                    val merged = if (page <= 1) {
                        mapped
                    } else {
                        (it.sessions + mapped).distinctBy { item -> item.sessionId }
                    }
                    it.copy(
                        sessionsLoading = false,
                        sessions = merged,
                        sessionTotal = result.total
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                update { it.copy(sessionsLoading = false, sessionsError = e.readableMessage()) }
            }
        }
    }

    private fun AgentSessionDto.toHistoryItem(): AxingHistoryItem {
        val time = lastMessageTime.takeIf { it.isNotBlank() }?.let { " · ${shortDate(it)}" }.orEmpty()
        return AxingHistoryItem(
            sessionId = sessionId,
            title = title.ifBlank { "未命名对话" },
            meta = sceneLabel(scene) + time,
            scene = scene
        )
    }

    private fun shortDate(value: String): String =
        if (value.length >= 16) value.substring(5, 16) else value

    /** 打开历史会话：按 sessionId 拉真实消息（默认最近 30 条，可向上翻页） */
    fun openHistory(item: AxingHistoryItem) {
        replyJob?.cancel()
        update {
            it.copy(
                messages = emptyList(),
                input = "",
                imageUri = null,
                capability = null,
                showDrawer = false,
                sessionId = item.sessionId,
                sessionScene = item.scene,
                isThinking = true,
                thinkingLabel = "正在载入历史对话…",
                streamingText = "",
                liveSteps = emptyList(),
                liveNote = null,
                activeTaskId = 0,
                hasEarlier = false,
                firstMessageId = 0
            )
        }
        replyJob = viewModelScope.launch {
            try {
                val list = agentRepository.messages(item.sessionId)
                val actSteps = loadActSteps(list)
                val ui = list.mapNotNull { message -> message.toUiMessage(item.scene, actSteps) }
                update {
                    it.copy(
                        isThinking = false,
                        messages = ui,
                        hasEarlier = list.size >= HISTORY_PAGE_SIZE,
                        firstMessageId = list.firstOrNull()?.messageId ?: 0
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                update {
                    it.copy(
                        isThinking = false,
                        messages = listOf(AxingMessage.Error(e.readableMessage()))
                    )
                }
            }
        }
    }

    /**
     * 历史里的执行任务消息只带 taskId，没有步骤；
     * 这里按 taskId 把 `GET /agent/tasks/{taskId}/steps` 的步骤补回来，还原执行过程。
     */
    private suspend fun loadActSteps(messages: List<AgentMessageDto>): Map<Long, List<AgentTaskStepDto>> {
        val result = mutableMapOf<Long, List<AgentTaskStepDto>>()
        messages.filter {
            it.task?.taskType == AgentTaskDto.TYPE_ACT && it.taskId > 0
        }.distinctBy { it.taskId }.take(8).forEach { message ->
            val steps = runCatching { agentRepository.taskSteps(message.taskId).steps }.getOrNull()
            if (!steps.isNullOrEmpty()) result[message.taskId] = steps
        }
        return result
    }

    /** 向上翻更早的消息（走 beforeMessageId 游标） */
    fun loadEarlier() {
        val current = _state.value
        if (current.loadingEarlier || !current.hasEarlier || current.firstMessageId <= 0) return
        if (current.sessionId <= 0) return
        update { it.copy(loadingEarlier = true) }
        viewModelScope.launch {
            try {
                val list = agentRepository.messages(current.sessionId, current.firstMessageId)
                val actSteps = loadActSteps(list)
                val older = list.mapNotNull { message ->
                    message.toUiMessage(current.sessionScene, actSteps)
                }
                update {
                    it.copy(
                        loadingEarlier = false,
                        messages = older + it.messages,
                        hasEarlier = list.size >= HISTORY_PAGE_SIZE,
                        firstMessageId = list.firstOrNull()?.messageId ?: it.firstMessageId
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                update { it.copy(loadingEarlier = false) }
                notify(e.readableMessage())
            }
        }
    }

    // ------------------------------------------------------------ 会话重命名 / 删除

    fun openSessionMenu(item: AxingHistoryItem) {
        update { it.copy(sessionMenu = item) }
    }

    fun closeSessionMenu() {
        update { it.copy(sessionMenu = null) }
    }

    fun startRename(item: AxingHistoryItem) {
        update {
            it.copy(
                sessionMenu = null,
                renameTarget = item,
                renameDraft = item.title
            )
        }
    }

    fun setRenameDraft(value: String) {
        update { it.copy(renameDraft = value) }
    }

    fun cancelRename() {
        update { it.copy(renameTarget = null, renameDraft = "") }
    }

    fun confirmRename() {
        val target = _state.value.renameTarget ?: return
        val title = _state.value.renameDraft.trim()
        if (title.isEmpty()) {
            notify("会话标题不能为空")
            return
        }
        update { it.copy(renameTarget = null, renameDraft = "") }
        viewModelScope.launch {
            try {
                agentRepository.renameSession(target.sessionId, title)
                update { state ->
                    state.copy(
                        sessions = state.sessions.map {
                            if (it.sessionId == target.sessionId) it.copy(title = title) else it
                        }
                    )
                }
                notify("已重命名")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                notify(e.readableMessage())
            }
        }
    }

    fun startDelete(item: AxingHistoryItem) {
        update { it.copy(sessionMenu = null, deleteTarget = item) }
    }

    fun cancelDelete() {
        update { it.copy(deleteTarget = null) }
    }

    fun confirmDelete() {
        val target = _state.value.deleteTarget ?: return
        update { it.copy(deleteTarget = null) }
        viewModelScope.launch {
            try {
                agentRepository.deleteSession(target.sessionId)
                update { state ->
                    state.copy(
                        sessions = state.sessions.filterNot { it.sessionId == target.sessionId },
                        sessionTotal = (state.sessionTotal - 1).coerceAtLeast(0)
                    )
                }
                if (_state.value.sessionId == target.sessionId) newChat()
                notify("已删除会话")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                notify(e.readableMessage())
            }
        }
    }

    fun newChat() {
        cancelRunningTask()
        replyJob?.cancel()
        update {
            it.copy(
                messages = emptyList(),
                input = "",
                capability = null,
                imageUri = null,
                isThinking = false,
                streamingText = "",
                liveSteps = emptyList(),
                liveNote = null,
                activeTaskId = 0,
                cancelling = false,
                showDrawer = false,
                sessionId = 0,
                sessionScene = "chat",
                hasEarlier = false,
                firstMessageId = 0
            )
        }
    }

    // ------------------------------------------------------------ 长期记忆

    fun openMemories() {
        update { it.copy(showDrawer = false, memory = it.memory.copy(visible = true)) }
        loadMemories()
    }

    fun closeMemories() {
        memoryJob?.cancel()
        update { it.copy(memory = AxingMemoryUiState()) }
    }

    fun retryMemories() = loadMemories()

    private fun loadMemories() {
        if (_state.value.memory.loading) return
        update { it.copy(memory = it.memory.copy(loading = true, error = null)) }
        memoryJob = viewModelScope.launch {
            try {
                val list = agentRepository.memories()
                update {
                    it.copy(memory = it.memory.copy(loading = false, items = list, error = null))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                update {
                    it.copy(memory = it.memory.copy(loading = false, error = e.readableMessage()))
                }
            }
        }
    }

    fun setMemoryDraft(value: String) {
        update { it.copy(memory = it.memory.copy(draft = value.take(MEMORY_MAX_LENGTH))) }
    }

    fun editMemory(memory: AgentMemoryDto) {
        update {
            it.copy(
                memory = it.memory.copy(
                    editingId = memory.memoryId,
                    draft = memory.content.take(MEMORY_MAX_LENGTH)
                )
            )
        }
    }

    fun cancelMemoryEdit() {
        update { it.copy(memory = it.memory.copy(editingId = 0, draft = "")) }
    }

    fun saveMemoryDraft() {
        val current = _state.value.memory
        val content = current.draft.trim()
        if (content.isEmpty()) {
            notify("记忆内容不能为空")
            return
        }
        if (current.busy) return
        update { it.copy(memory = it.memory.copy(busy = true, error = null)) }
        viewModelScope.launch {
            try {
                if (current.editingId > 0) {
                    agentRepository.updateMemory(current.editingId, content)
                } else {
                    agentRepository.addMemory(content, "app")
                }
                update {
                    it.copy(
                        memory = it.memory.copy(
                            busy = false,
                            draft = "",
                            editingId = 0,
                            error = null
                        )
                    )
                }
                notify("已保存")
                loadMemories()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                update {
                    it.copy(memory = it.memory.copy(busy = false, error = e.readableMessage()))
                }
            }
        }
    }

    fun deleteMemory(memoryId: Long) {
        viewModelScope.launch {
            try {
                agentRepository.deleteMemory(memoryId)
                update { state ->
                    state.copy(memory = state.memory.copy(items = state.memory.items.filterNot { it.memoryId == memoryId }))
                }
                notify("已删除记忆")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                notify(e.readableMessage())
            }
        }
    }

    /** 模型提议的记忆：保存后调用 POST /agent/memories（后端按内容幂等） */
    fun saveMemoryCandidate(candidateId: String, content: String) {
        if (_state.value.candidateStates[candidateId] != null) return
        viewModelScope.launch {
            try {
                agentRepository.addMemory(content, "app")
                update { it.copy(candidateStates = it.candidateStates + (candidateId to "saved")) }
                notify("已记住这条信息")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                notify(e.readableMessage())
            }
        }
    }

    fun ignoreMemoryCandidate(candidateId: String) {
        update { it.copy(candidateStates = it.candidateStates + (candidateId to "ignored")) }
    }

    // ------------------------------------------------------------ 发送

    fun send() {
        val current = _state.value
        if (current.isThinking) return
        val imageUri = current.imageUri
        // 只点了能力、没有输入时，气泡里补一句可读的话；真正提交的标题类参数仍用 typed
        val typed = current.input.trim()
        val text = typed.ifBlank {
            when {
                imageUri != null -> "请描述这张图片的内容"
                current.capability != null -> "帮我${current.capability.label}"
                else -> return
            }
        }
        val capability = current.capability ?: inferCapability(text)
        update {
            it.copy(
                messages = it.messages + AxingMessage.User(text, imageUri),
                input = "",
                imageUri = null,
                capability = null,
                isThinking = true,
                thinkingLabel = thinkingLabelOf(capability),
                streamingText = "",
                liveSteps = emptyList(),
                liveNote = null,
                activeTaskId = 0,
                cancelling = false
            )
        }
        dispatch(text, capability, imageUri, typed = typed)
    }

    fun regenerate() {
        val current = _state.value
        if (current.isThinking) return
        val lastUser = current.messages.lastOrNull { it is AxingMessage.User } as? AxingMessage.User
            ?: return
        val capability = (current.messages.lastOrNull { it is AxingMessage.Assistant }
            as? AxingMessage.Assistant)?.answer?.capability ?: inferCapability(lastUser.text)
        replyJob?.cancel()
        update {
            it.copy(
                messages = it.messages.dropLastWhile {
                    it is AxingMessage.Assistant || it is AxingMessage.Error
                },
                isThinking = true,
                thinkingLabel = thinkingLabelOf(capability),
                streamingText = "",
                liveSteps = emptyList(),
                liveNote = null,
                activeTaskId = 0
            )
        }
        dispatch(lastUser.text, capability, lastUser.imageUri, typed = lastUser.text, retry = true)
    }

    /** 回答卡上的「做成 PPT」：用当前课程生成，不需要用户再输入 */
    fun makePpt() {
        if (_state.value.isThinking) return
        val course = _state.value.course
        val text = course?.let { "《${it.title}》课程汇报" } ?: "课程学习总结"
        update {
            it.copy(
                messages = it.messages + AxingMessage.User("把课程内容做成一份 PPT"),
                capability = null,
                isThinking = true,
                thinkingLabel = thinkingLabelOf(AxingCapability.PPT),
                activeTaskId = 0
            )
        }
        launchAnswer { answerPpt(text) }
    }

    fun openDeck(deck: AxingDeck) {
        update { it.copy(fullscreenDeck = deck) }
    }

    fun closeDeck() {
        update { it.copy(fullscreenDeck = null) }
    }

    /** 停止正在执行的长任务：只请求取消，轮询会读到 CANCELED */
    fun cancelRunningTask() {
        val taskId = _state.value.activeTaskId
        if (taskId <= 0 || _state.value.cancelling) return
        update { it.copy(cancelling = true) }
        viewModelScope.launch {
            try {
                val canceled = agentRepository.cancelTask(taskId)
                notify(if (canceled) "已请求停止任务" else "任务已结束")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                notify(e.readableMessage())
            } finally {
                update { it.copy(cancelling = false) }
            }
        }
    }

    private fun dispatch(
        text: String,
        capability: AxingCapability,
        imageUri: String?,
        typed: String = "",
        retry: Boolean = false
    ) {
        launchAnswer {
            if (imageUri != null) {
                // 只有 /chat 支持图片，图片提问统一走课程问答
                answerChat(text, AxingCapability.QA, imageUri)
                return@launchAnswer
            }
            when (capability) {
                AxingCapability.ACT -> answerAct(
                    goal = text,
                    // 重新生成时换一个幂等键，避免命中 5 分钟内的旧任务
                    idempotencyKey = if (retry) "app-${System.currentTimeMillis()}" else ""
                )
                AxingCapability.DATA -> answerSql(text)
                AxingCapability.SUBTITLE -> answerSubtitle()
                AxingCapability.VIDEO -> answerVideo()
                AxingCapability.PPT -> answerPpt(typed.ifBlank { text })
                AxingCapability.STUDY -> answerStudy()
                AxingCapability.SUMMARY, AxingCapability.QA -> answerChat(text, capability, null)
            }
        }
    }

    private fun launchAnswer(block: suspend () -> Unit) {
        replyJob?.cancel()
        replyJob = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                update {
                    it.copy(
                        isThinking = false,
                        streamingText = "",
                        liveSteps = emptyList(),
                        liveNote = null,
                        activeTaskId = 0,
                        messages = it.messages + AxingMessage.Error(e.readableMessage())
                    )
                }
            }
        }
    }

    private fun pushAnswer(answer: AxingAnswer) {
        update {
            it.copy(
                isThinking = false,
                streamingText = "",
                liveSteps = emptyList(),
                liveNote = null,
                activeTaskId = 0,
                cancelling = false,
                messages = it.messages + AxingMessage.Assistant(answer)
            )
        }
    }

    private fun thinkingLabelOf(capability: AxingCapability): String = when (capability) {
        AxingCapability.VIDEO -> "正在分析视频…"
        AxingCapability.PPT -> "正在生成幻灯片…"
        AxingCapability.STUDY -> "正在统计学习数据…"
        AxingCapability.ACT -> "阿星正在执行任务…"
        AxingCapability.DATA -> "正在查询数据…"
        AxingCapability.SUBTITLE -> "正在转写字幕…"
        AxingCapability.SUMMARY, AxingCapability.QA -> "阿星正在整理回答…"
    }

    // ------------------------------------------------------------ 各能力实现

    /** 会话延续：只有场景匹配时才复用后端 sessionId，避免两种问答串到一个会话里 */
    private fun continuationId(vararg scenes: String): Long? =
        _state.value.sessionId.takeIf { it > 0 && _state.value.sessionScene in scenes }

    private suspend fun answerChat(
        text: String,
        capability: AxingCapability,
        imageUri: String?
    ) {
        val course = _state.value.course
        // 社区会话不能当作课程会话续用
        val sessionId = continuationId("chat", "course")
        val imageUrls = imageUri?.let { listOf(uploadChatImage(it)) }.orEmpty()

        val result = if (imageUrls.isEmpty() && _state.value.streamingReply) {
            agentRepository.chatStreaming(
                question = text,
                courseId = course?.id?.takeIf { it > 0 },
                sessionId = sessionId,
                onDelta = { partial -> update { it.copy(streamingText = partial) } }
            )
        } else {
            agentRepository.chat(
                question = text,
                courseId = course?.id?.takeIf { it > 0 },
                sessionId = sessionId,
                imageUrls = imageUrls
            )
        }

        if (result.answer.isBlank() && result.refs.isEmpty() && result.memoryCandidates.isEmpty()) {
            throw ApiException(-1, "阿星没有返回内容，请换个问法再试")
        }
        update {
            it.copy(
                sessionId = result.sessionId.takeIf { id -> id > 0 } ?: it.sessionId,
                sessionScene = if (sessionId != null) it.sessionScene else "course",
                streamingText = ""
            )
        }
        val blocks = buildList {
            if (result.answer.isNotBlank()) add(AxingBlock.Paragraph(result.answer))
            addRefBlocks(result.refs)
            if (result.memoryCandidates.isNotEmpty()) {
                add(
                    AxingBlock.MemoryOptions(
                        result.memoryCandidates.map {
                            AxingMemoryOption(it.candidateId, it.content, it.reason)
                        }
                    )
                )
            }
        }
        pushAnswer(
            AxingAnswer(
                capability = capability,
                course = course,
                heading = null,
                blocks = blocks,
                caption = when {
                    result.memoryCandidates.isNotEmpty() -> "上面的信息可以保存为长期记忆，下次对话我会记得。"
                    result.refs.isEmpty() -> null
                    result.refs.any { it.isPost } -> "来源：社区帖子检索"
                    else -> "来源：课程知识库检索"
                }
            )
        )
    }

    /**
     * 执行型智能体：提交目标 → 轮询步骤 → 展示过程与最终回答。
     *
     * 后端 RUNNING 期间就能拿到已完成步骤，所以边轮询边更新 [AxingUiState.liveSteps]，
     * 让用户看到「查课程原文 → 查社区帖子 → 生成 PPT」的真实过程。
     */
    private suspend fun answerAct(goal: String, idempotencyKey: String = "") {
        val start = agentRepository.act(
            goal = goal,
            sessionId = continuationId("act", "chat"),
            maxSteps = 8,
            idempotencyKey = idempotencyKey
        )
        update {
            it.copy(
                sessionId = start.sessionId.takeIf { id -> id > 0 } ?: it.sessionId,
                sessionScene = "act",
                activeTaskId = start.taskId,
                thinkingLabel = if (start.idempotent) "接管 5 分钟内提交过的任务…" else "阿星正在执行任务…",
                liveSteps = emptyList(),
                liveNote = null
            )
        }
        val detail = try {
            agentRepository.awaitAct(start.taskId) { current ->
                applyActProgress(current)
            }
        } finally {
            update { it.copy(activeTaskId = 0) }
        }

        val course = _state.value.course
        val blocks = buildList {
            val steps = detail.steps.map(::toStepUi)
            if (steps.isNotEmpty()) add(AxingBlock.Steps(steps, actStepNote(detail)))
            when {
                detail.status == AgentTaskDto.STATUS_CANCELED ->
                    add(AxingBlock.Paragraph("已停止执行，已完成的步骤保留在下面。"))
                detail.answer.isNotBlank() -> add(AxingBlock.Paragraph(detail.answer))
                detail.status == AgentTaskDto.STATUS_FAILED ->
                    add(AxingBlock.Paragraph(detail.errorMsg.ifBlank { "任务执行失败，请稍后重试" }))
            }
            addRefBlocks(detail.refs)
        }
        pushAnswer(
            AxingAnswer(
                capability = AxingCapability.ACT,
                course = course,
                heading = when (detail.status) {
                    AgentTaskDto.STATUS_CANCELED -> "已取消执行"
                    AgentTaskDto.STATUS_FAILED -> "执行未完成"
                    else -> null
                },
                blocks = blocks,
                caption = actCaption(detail)
            )
        )
    }

    private fun applyActProgress(detail: AgentActDetail) {
        update {
            it.copy(
                liveSteps = detail.steps.map(::toStepUi),
                liveNote = detail.currentStep.takeIf { step -> step.isNotBlank() },
                thinkingLabel = detail.currentStep.takeIf { step -> step.isNotBlank() }
                    ?: "阿星正在执行任务…"
            )
        }
    }

    private fun actStepNote(detail: AgentActDetail): String? = when {
        detail.status == AgentTaskDto.STATUS_CANCELED -> "任务已取消，以上为已完成步骤"
        detail.truncated -> "已达到执行上限，结果可能不完整"
        detail.degraded && detail.degradeReason.isNotBlank() -> detail.degradeReason
        else -> null
    }

    private fun actCaption(detail: AgentActDetail): String? = buildString {
        if (detail.maxSteps > 0) {
            append("推理 ${detail.rounds}/${detail.maxSteps} 轮")
            if (detail.toolCallCount > 0) append(" · 工具调用 ${detail.toolCallCount} 次")
        }
        if (detail.errorCode.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(detail.errorCode)
        }
    }.ifBlank { null }

    /** SQL 数据查询：只读视图，结果落 table 消息 */
    private suspend fun answerSql(question: String) {
        val result = agentRepository.sqlAsk(question, continuationId("sql", "chat"))
        if (result.sessionId > 0) {
            update { it.copy(sessionId = result.sessionId, sessionScene = "sql") }
        }
        val blocks = buildList {
            if (result.answer.isNotBlank()) add(AxingBlock.Paragraph(result.answer))
            if (result.headers.isNotEmpty()) {
                add(
                    AxingBlock.Table(
                        AxingTable(
                            headers = result.headers,
                            rows = result.rows.take(TABLE_PREVIEW_ROWS),
                            rowCount = result.rowCount,
                            sql = result.sql,
                            error = result.error
                        )
                    )
                )
            } else if (result.error) {
                add(AxingBlock.Paragraph("查询没有通过只读视图校验，请换个问法。"))
            } else {
                add(AxingBlock.Paragraph("没有查询到符合条件的数据。"))
            }
        }
        pushAnswer(
            AxingAnswer(
                capability = AxingCapability.DATA,
                course = null,
                heading = "数据查询结果",
                blocks = blocks,
                caption = if (result.rows.isNotEmpty()) {
                    "结果来自只读统计视图 · 共 ${result.rowCount} 行，列表最多展示 $TABLE_PREVIEW_ROWS 行"
                } else {
                    "结果来自只读统计视图"
                }
            )
        )
    }

    /** 视频字幕：只跑 ASR，输出 SRT/VTT 直链 */
    private suspend fun answerSubtitle(force: Boolean = false) {
        val videoId = _state.value.videoId
        if (videoId <= 0) {
            pushAnswer(
                AxingAnswer(
                    capability = AxingCapability.SUBTITLE,
                    course = _state.value.course,
                    heading = null,
                    blocks = listOf(
                        AxingBlock.Paragraph(
                            "生成字幕需要带上具体小节：打开课程视频，点右上角的「问阿星」，" +
                                "我就能对这一节做语音识别并生成 SRT / VTT 字幕。"
                        )
                    )
                )
            )
            return
        }
        val start = agentRepository.startVideoSubtitle(
            videoId = videoId,
            sessionId = continuationId("video", "chat"),
            force = force
        )
        update {
            it.copy(
                sessionId = start.sessionId.takeIf { id -> id > 0 } ?: it.sessionId,
                sessionScene = "video",
                activeTaskId = start.taskId,
                thinkingLabel = "正在转写字幕…"
            )
        }
        val task = try {
            agentRepository.awaitTask(start.taskId) { current ->
                update { it.copy(thinkingLabel = progressLabel("正在转写字幕", current.progress)) }
            }
        } finally {
            update { it.copy(activeTaskId = 0) }
        }
        if (task.status == AgentTaskDto.STATUS_CANCELED) {
            pushAnswer(
                AxingAnswer(
                    capability = AxingCapability.SUBTITLE,
                    course = _state.value.course,
                    heading = "已取消字幕生成",
                    blocks = listOf(AxingBlock.Paragraph("已停止语音识别，可以重新发起。"))
                )
            )
            return
        }
        if (!task.isSuccess) {
            throw ApiException(-1, task.errorMsg.ifBlank { "字幕生成失败，请稍后重试" })
        }
        val subtitle = AgentRepository.parseSubtitle(task.outputJson)
            ?: throw ApiException(-1, "字幕结果为空")
        pushAnswer(
            AxingAnswer(
                capability = AxingCapability.SUBTITLE,
                course = _state.value.course,
                heading = "《${subtitle.videoTitle.ifBlank { "当前视频" }}》字幕",
                blocks = listOf(AxingBlock.SubtitleCard(subtitle.toUiSubtitle())),
                caption = if (subtitle.cached) {
                    "命中已生成的字幕，没有重新转写。"
                } else {
                    "由视频语音识别生成${if (subtitle.subtitleSegments > 0) "，共 ${subtitle.subtitleSegments} 句" else ""}。"
                }
            )
        )
    }

    private suspend fun answerStudy() {
        val stats = studyRepository.getStats()
        val weekMinutes = stats.weekMinutes.sumOf { it.minutes }
        val summary = if (weekMinutes > 0) {
            "最近 7 天累计学习 ${durationText(weekMinutes)}，连续学习 ${stats.continuousDays} 天。"
        } else {
            "最近 7 天还没有学习记录，先去看一节课吧。"
        }
        val bullets = buildList {
            add("今日学习 ${stats.todayMinutes} 分钟。")
            stats.todayCourseMinutes?.let { add("今日课程 $it 分钟。") }
            stats.todayArticleMinutes?.let { add("今日阅读文章 $it 分钟。") }
            val learning = stats.learningCourses?.let { "，在学 $it 门" }.orEmpty()
            add("已完成课程 ${stats.completedCourses} 门$learning。")
            stats.readPostsWeek?.let { add("本周阅读文章 $it 篇。") }
            add("累计笔记 ${stats.noteCount} 条。")
        }
        pushAnswer(
            AxingAnswer(
                capability = AxingCapability.STUDY,
                course = null,
                heading = "最近 7 天学习数据",
                blocks = listOf(
                    AxingBlock.Paragraph(summary),
                    AxingBlock.Bullets(bullets)
                ),
                caption = "数据来自学习记录接口"
            )
        )
    }

    private suspend fun answerVideo() {
        val videoId = _state.value.videoId
        if (videoId <= 0) {
            pushAnswer(
                AxingAnswer(
                    capability = AxingCapability.VIDEO,
                    course = _state.value.course,
                    heading = null,
                    blocks = listOf(
                        AxingBlock.Paragraph(
                            "视频总结需要带上具体小节：打开课程视频，点右上角的「问阿星」，" +
                                "我就能读到当前这一节的转写内容。"
                        )
                    )
                )
            )
            return
        }
        val start = agentRepository.startVideoSummary(
            videoId = videoId,
            sessionId = continuationId("video", "chat")
        )
        update {
            it.copy(
                sessionId = start.sessionId.takeIf { id -> id > 0 } ?: it.sessionId,
                sessionScene = "video",
                activeTaskId = start.taskId,
                thinkingLabel = "正在分析视频…"
            )
        }
        val task = try {
            agentRepository.awaitTask(start.taskId) { current ->
                update { it.copy(thinkingLabel = progressLabel("正在分析视频", current.progress)) }
            }
        } finally {
            update { it.copy(activeTaskId = 0) }
        }
        if (task.status == AgentTaskDto.STATUS_CANCELED) {
            pushAnswer(
                AxingAnswer(
                    capability = AxingCapability.VIDEO,
                    course = _state.value.course,
                    heading = "已取消总结",
                    blocks = listOf(AxingBlock.Paragraph("已停止分析视频，可以重新发起。"))
                )
            )
            return
        }
        if (!task.isSuccess) {
            throw ApiException(-1, task.errorMsg.ifBlank { "视频总结失败，请稍后重试" })
        }
        val summary = AgentRepository.parseVideoSummary(task.outputJson)
            ?: throw ApiException(-1, "视频总结结果为空")
        val title = summary.videoTitle.ifBlank { "当前视频" }
        val heading = if (summary.durationMinutes > 0) {
            "《$title》· ${summary.durationMinutes} 分钟视频总结"
        } else {
            "《$title》视频总结"
        }
        val blocks = buildList {
            add(AxingBlock.Paragraph(summary.summary.ifBlank { "这一节还没有可用的转写内容。" }))
            if (summary.subtitleUrl.isNotBlank()) {
                add(
                    AxingBlock.SubtitleCard(
                        AxingSubtitle(
                            title = "$title 字幕",
                            srtUrl = summary.subtitleUrl,
                            vttUrl = summary.subtitleVttUrl,
                            format = summary.subtitleFormat.ifBlank { "srt" },
                            segments = summary.subtitleSegments
                        )
                    )
                )
            }
        }
        pushAnswer(
            AxingAnswer(
                capability = AxingCapability.VIDEO,
                course = _state.value.course,
                heading = heading,
                blocks = blocks,
                caption = if (summary.fallback) {
                    "没有可用的字幕，以上内容基于课程信息生成。"
                } else {
                    "由视频转写内容生成${if (summary.subtitleUrl.isNotBlank()) "，字幕可直接下载" else ""}。"
                }
            )
        )
    }

    private suspend fun answerPpt(topic: String) {
        val course = _state.value.course
        // 用户可能输入「把 Java 集合做成一份 PPT」：去掉前缀与动宾尾巴后只留标题
        val cleaned = topic.trim()
            .replace(PPT_PREFIX, "")
            .replace(PPT_SUFFIX, "")
            .trim()
        val title = cleaned.takeIf { it.isNotBlank() && it.length <= 100 }
            ?: course?.title?.takeIf { it.isNotBlank() }?.let { "《$it》课程汇报" }
            ?: "课程学习总结"
        val start = agentRepository.startPpt(
            topic = title,
            videoId = _state.value.videoId.takeIf { it > 0 },
            sessionId = continuationId("ppt", "chat")
        )
        update {
            it.copy(
                sessionId = start.sessionId.takeIf { id -> id > 0 } ?: it.sessionId,
                sessionScene = "ppt",
                activeTaskId = start.taskId,
                thinkingLabel = "正在生成幻灯片…"
            )
        }
        val task = try {
            agentRepository.awaitTask(start.taskId) { current ->
                update { it.copy(thinkingLabel = progressLabel("正在生成幻灯片", current.progress)) }
            }
        } finally {
            update { it.copy(activeTaskId = 0) }
        }
        if (task.status == AgentTaskDto.STATUS_CANCELED) {
            pushAnswer(
                AxingAnswer(
                    capability = AxingCapability.PPT,
                    course = course,
                    heading = "已取消生成",
                    blocks = listOf(AxingBlock.Paragraph("已停止生成 PPT，可以重新发起。"))
                )
            )
            return
        }
        if (!task.isSuccess) {
            throw ApiException(-1, task.errorMsg.ifBlank { "PPT 生成失败，请稍后重试" })
        }
        val ppt = AgentRepository.parsePpt(task.outputJson)
            ?: throw ApiException(-1, "PPT 生成结果为空")
        val deck = ppt.toDeck(course)
        pushAnswer(
            AxingAnswer(
                capability = AxingCapability.PPT,
                course = course,
                heading = null,
                blocks = listOf(AxingBlock.Deck(deck)),
                caption = buildString {
                    append("共 ${deck.slides.size} 页 · 可全屏演示")
                    if (deck.fileUrl.isNotBlank()) append("，导出可下载 PPTX 文件。") else append("。")
                }
            )
        )
    }

    // ------------------------------------------------------------ 历史消息还原

    private fun AgentMessageDto.toUiMessage(
        scene: String,
        actSteps: Map<Long, List<AgentTaskStepDto>> = emptyMap()
    ): AxingMessage? = when (role) {
        "user" -> AxingMessage.User(
            text = content,
            imageUri = images.firstOrNull()
        )
        "assistant" -> AxingMessage.Assistant(
            AxingAnswer(
                capability = capabilityOf(scene, contentType, task?.taskType),
                course = null,
                heading = null,
                blocks = messageBlocks(this, scene, actSteps),
                caption = null
            )
        )
        else -> null
    }

    private fun capabilityOf(scene: String, contentType: String, taskType: String?): AxingCapability =
        when {
            taskType == AgentTaskDto.TYPE_PPT || scene == "ppt" -> AxingCapability.PPT
            taskType == AgentTaskDto.TYPE_VIDEO_SUMMARY ||
                taskType == AgentTaskDto.TYPE_VIDEO_SUBTITLE || scene == "video" -> AxingCapability.VIDEO
            taskType == AgentTaskDto.TYPE_ACT || scene == "act" -> AxingCapability.ACT
            scene == "sql" || contentType == "table" -> AxingCapability.DATA
            scene == "community" -> AxingCapability.QA
            else -> AxingCapability.QA
        }

    private fun messageBlocks(
        message: AgentMessageDto,
        scene: String,
        actSteps: Map<Long, List<AgentTaskStepDto>> = emptyMap()
    ): List<AxingBlock> = buildList {
        if (message.contentType == "table") {
            val snapshot = AgentRepository.parseSqlSnapshot(message.resultJson)
            if (message.content.isNotBlank()) add(AxingBlock.Paragraph(message.content))
            if (snapshot != null && snapshot.headers.isNotEmpty()) {
                add(
                    AxingBlock.Table(
                        AxingTable(
                            headers = snapshot.headers,
                            rows = snapshot.rows.take(TABLE_PREVIEW_ROWS),
                            rowCount = snapshot.rowCount,
                            sql = snapshot.sql,
                            error = snapshot.error
                        )
                    )
                )
            }
            return@buildList
        }
        if (message.contentType == "task") {
            val task = message.task
            if (task != null) {
                addTaskResultBlocks(task, actSteps[task.taskId].orEmpty())
            } else if (message.content.isNotBlank()) {
                add(AxingBlock.Paragraph(message.content))
            }
            return@buildList
        }
        if (message.content.isNotBlank()) add(AxingBlock.Paragraph(message.content))
        addRefBlocks(message.refs)
        if (scene == "act") {
            // 老数据没有 refs 字段时，从 outputJson 里再补一次
            message.task?.outputJson?.let { raw ->
                val output = AgentRepository.parseActOutput(raw)
                if (output != null && output.refs.isNotEmpty() && message.refs.isEmpty()) {
                    addRefBlocks(output.refs)
                }
            }
        }
    }

    private fun MutableList<AxingBlock>.addTaskResultBlocks(
        task: AgentTaskDto,
        steps: List<AgentTaskStepDto> = emptyList()
    ) {
        val title = taskTitle(task.taskType)
        when (task.status) {
            AgentTaskDto.STATUS_SUCCESS -> when (task.taskType) {
                AgentTaskDto.TYPE_PPT -> {
                    val ppt = AgentRepository.parsePpt(task.outputJson)
                    if (ppt != null) {
                        add(AxingBlock.Deck(ppt.toDeck(null)))
                    } else {
                        add(taskCardBlock(title, task, "结果解析失败"))
                    }
                }
                AgentTaskDto.TYPE_VIDEO_SUMMARY -> {
                    val summary = AgentRepository.parseVideoSummary(task.outputJson)
                    if (summary != null) {
                        add(AxingBlock.Paragraph(summary.summary))
                        if (summary.subtitleUrl.isNotBlank()) {
                            add(
                                AxingBlock.SubtitleCard(
                                    AxingSubtitle(
                                        title = summary.videoTitle.ifBlank { "视频" } + " 字幕",
                                        srtUrl = summary.subtitleUrl,
                                        vttUrl = summary.subtitleVttUrl,
                                        format = summary.subtitleFormat.ifBlank { "srt" },
                                        segments = summary.subtitleSegments
                                    )
                                )
                            )
                        }
                    } else {
                        add(taskCardBlock(title, task, "结果解析失败"))
                    }
                }
                AgentTaskDto.TYPE_VIDEO_SUBTITLE -> {
                    val subtitle = AgentRepository.parseSubtitle(task.outputJson)
                    if (subtitle != null) {
                        add(AxingBlock.SubtitleCard(subtitle.toUiSubtitle()))
                    } else {
                        add(taskCardBlock(title, task, "结果解析失败"))
                    }
                }
                AgentTaskDto.TYPE_ACT -> {
                    val output = AgentRepository.parseActOutput(task.outputJson)
                    if (steps.isNotEmpty()) {
                        add(AxingBlock.Steps(steps.map(::toStepUi)))
                    }
                    if (output != null) {
                        if (output.answer.isNotBlank()) add(AxingBlock.Paragraph(output.answer))
                        addRefBlocks(output.refs)
                        if (output.truncated) {
                            add(AxingBlock.Paragraph("已达到执行上限，结果可能不完整。"))
                        }
                    } else {
                        add(taskCardBlock(title, task, "结果解析失败"))
                    }
                }
                else -> add(taskCardBlock(title, task, ""))
            }
            AgentTaskDto.STATUS_FAILED ->
                add(taskCardBlock(title, task, task.errorMsg.ifBlank { "任务执行失败" }))
            AgentTaskDto.STATUS_CANCELED ->
                add(taskCardBlock(title, task, "任务已取消"))
            else ->
                add(taskCardBlock(title, task, "任务处理中，完成后可在此查看"))
        }
    }

    private fun taskCardBlock(title: String, task: AgentTaskDto, detail: String): AxingBlock =
        AxingBlock.TaskCard(
            AxingTaskInfo(
                title = title,
                status = task.status,
                detail = detail,
                errorMsg = task.errorMsg
            )
        )

    private fun taskTitle(taskType: String): String = when (taskType) {
        AgentTaskDto.TYPE_PPT -> "PPT 生成"
        AgentTaskDto.TYPE_VIDEO_SUMMARY -> "视频总结"
        AgentTaskDto.TYPE_VIDEO_SUBTITLE -> "视频字幕"
        AgentTaskDto.TYPE_ACT -> "执行任务"
        else -> "异步任务"
    }

    private fun MutableList<AxingBlock>.addRefBlocks(refs: List<AgentRef>) {
        if (refs.isEmpty()) return
        val posts = refs.filter { it.isPost }
        val courses = refs.filter { it.isCourse }
        if (posts.isNotEmpty()) {
            add(
                AxingBlock.Quotes(
                    posts.map {
                        AxingQuote(
                            title = it.postTitle.ifBlank { it.title.ifBlank { "相关帖子" } },
                            excerpt = it.excerpt.ifBlank { it.category.ifBlank { "社区帖子" } }
                        )
                    }
                )
            )
        }
        if (courses.isNotEmpty()) {
            add(
                AxingBlock.Sources(
                    courses.map {
                        AxingSource(
                            chapter = it.chapterName.ifBlank { "相关章节" },
                            title = it.courseName.ifBlank { "课程" }
                        )
                    }
                )
            )
        }
    }

    // ------------------------------------------------------------ 步骤 / 进度

    private fun toStepUi(step: AgentTaskStepDto): AxingStepUi {
        val kind = when {
            step.isThought -> "thought"
            step.isTool -> "tool"
            step.isObservation -> "observation"
            else -> "other"
        }
        val title = when (kind) {
            "thought" -> "思考下一步"
            "tool" -> "调用「${toolLabel(step.toolName)}」"
            "observation" -> "「${toolLabel(step.toolName)}」返回"
            else -> "执行步骤"
        }
        return AxingStepUi(
            seq = step.seq,
            kind = kind,
            title = title,
            detail = stepDetail(step),
            status = step.status,
            elapsedText = step.elapsedMs?.takeIf { it > 0 }?.let { formatMillis(it) }.orEmpty()
        )
    }

    private fun stepDetail(step: AgentTaskStepDto): String = when {
        step.isObservation -> observationText(step.observationJson)
        step.isTool -> argsText(step.argsJson)
        // thought 的意图由后端落进 argsJson 的 content 字段
        step.isThought -> argsText(step.argsJson)
        else -> ""
    }

    /** observationJson 是字符串：优先给摘要/回答/错误原因，隐藏 sql 之类审计字段 */
    private fun observationText(raw: String): String {
        val obj = parseJsonObject(raw) ?: return ""
        val ok = (obj["ok"] as? JsonPrimitive)?.content == "true"
        if (!ok) {
            val message = (obj["errorMsg"] as? JsonPrimitive)?.content.orEmpty()
                .ifBlank { (obj["message"] as? JsonPrimitive)?.content.orEmpty() }
            val hint = (obj["hint"] as? JsonPrimitive)?.content.orEmpty()
            return listOf(message, hint).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "工具调用失败" }
        }
        val preferred = listOf("summary", "answer", "courses", "posts", "memoryId", "url", "fileName")
        val parts = preferred.mapNotNull { key ->
            when (val value = obj[key]) {
                null -> null
                is JsonPrimitive -> value.content.takeIf { it.isNotBlank() }
                is JsonArray -> "${value.size} 条"
                is JsonObject -> null
                else -> null
            }
        }
        return parts.joinToString(" · ").take(160)
    }

    /**
     * argsJson 只展示关键参数。
     *
     * thought 步骤的 argsJson 只有 `content`（模型这一步想做什么），
     * 单独处理，避免拼成 `content=...` 反而不好读。
     */
    private fun argsText(raw: String): String {
        val obj = parseJsonObject(raw) ?: return ""
        (obj["content"] as? JsonPrimitive)?.content
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { return it.take(160) }
        val keys = listOf("query", "question", "goal", "topic", "videoId", "maxPages", "pages")
        val parts = keys.mapNotNull { key ->
            (obj[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }?.let { "$key=$it" }
        }
        return parts.joinToString("，").take(160)
    }

    private fun parseJsonObject(raw: String): JsonObject? {
        if (raw.isBlank()) return null
        return runCatching {
            com.github.learningplatform.core.network.AppJson.parseToJsonElement(raw) as? JsonObject
        }.getOrNull()
    }

    private fun toolLabel(name: String): String = when (name) {
        "search_course" -> "检索课程资料"
        "ask_community" -> "检索社区帖子"
        "query_learning_stats" -> "查询学习统计"
        "summarize_video" -> "总结视频"
        "generate_ppt" -> "生成 PPT"
        "save_memory" -> "保存记忆"
        else -> name.ifBlank { "工具" }
    }

    private fun formatMillis(millis: Int): String =
        if (millis >= 1000) String.format("%.1fs", millis / 1000f) else "${millis}ms"

    private fun progressLabel(prefix: String, progress: Int): String =
        if (progress > 0) "$prefix $progress%" else "$prefix…"

    // ------------------------------------------------------------ 图片

    private suspend fun uploadChatImage(localUri: String): String {
        // 历史 / 重试时可能已经是远端地址，直接复用
        if (localUri.startsWith("http://") || localUri.startsWith("https://")) return localUri
        val file = fileRepository.uriToCacheFile(Uri.parse(localUri))
        return try {
            agentRepository.uploadChatImage(file)
        } finally {
            runCatching { file.delete() }
        }
    }

    private fun AgentPptResultDto.toDeck(course: AxingCourse?): AxingDeck {
        val fallbackTitle = course?.title?.let { "《$it》课程汇报" } ?: "课程汇报"
        val deckTitle = outline.title.ifBlank { fallbackTitle }

        // 新版任务返回结构化页；旧任务 slides 是数字，按 outline 降级
        if (slides.isNotEmpty()) {
            val ui = slides.map { slide ->
                AxingSlide(
                    eyebrow = slideTypeLabel(slide.type),
                    title = slide.title.ifBlank { deckTitle },
                    subtitle = "",
                    bullets = slide.bullets.filter { it.isNotBlank() },
                    footer = "阿星 · 学习总结"
                )
            }
            return AxingDeck(
                title = deckTitle,
                courseTitle = course?.title.orEmpty(),
                slides = ui,
                fileUrl = url,
                fileName = fileName
            )
        }

        val bodySlides = buildList {
            outline.sections.forEachIndexed { index, section ->
                add(
                    AxingSlide(
                        eyebrow = "0${index + 1} 章节",
                        title = section.title.ifBlank { "第 ${index + 1} 部分" },
                        subtitle = section.summary,
                        bullets = emptyList(),
                        footer = "阿星 · 学习总结"
                    )
                )
            }
            if (outline.keyPoints.isNotEmpty()) {
                add(
                    AxingSlide(
                        eyebrow = "关键要点",
                        title = "记住这几条",
                        subtitle = "",
                        bullets = outline.keyPoints,
                        footer = "阿星 · 学习总结"
                    )
                )
            }
            if (outline.closing.isNotBlank() || outline.highlights.isNotEmpty()) {
                add(
                    AxingSlide(
                        eyebrow = "结语",
                        title = outline.closing.ifBlank { "把结论用到下一次学习" },
                        subtitle = "",
                        bullets = outline.highlights,
                        footer = "阿星 · 学习总结"
                    )
                )
            }
        }
        val cover = AxingSlide(
            eyebrow = "阿星",
            title = deckTitle,
            subtitle = outline.subtitle,
            bullets = emptyList(),
            footer = "共 ${bodySlides.size + 1} 页"
        )
        return AxingDeck(
            title = deckTitle,
            courseTitle = course?.title.orEmpty(),
            slides = listOf(cover) + bodySlides,
            fileUrl = url,
            fileName = fileName
        )
    }

    private fun slideTypeLabel(type: String): String = when (type) {
        "cover" -> "封面"
        "contents" -> "目录"
        "section" -> "章节"
        "highlights" -> "核心亮点"
        "keyPoints" -> "关键要点"
        "closing" -> "结语"
        else -> "内容"
    }

    private fun AgentSubtitleDto.toUiSubtitle(): AxingSubtitle = AxingSubtitle(
        title = videoTitle.ifBlank { "视频字幕" },
        srtUrl = subtitleUrl,
        vttUrl = subtitleVttUrl,
        format = subtitleFormat.ifBlank { "srt" },
        segments = subtitleSegments,
        cached = cached
    )

    private fun durationText(minutes: Int): String {
        if (minutes <= 0) return "0 分钟"
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours > 0 && rest > 0 -> "$hours 小时 $rest 分钟"
            hours > 0 -> "$hours 小时"
            else -> "$rest 分钟"
        }
    }

    private companion object {
        /** 与后端一致：会话消息默认一页 30 条 */
        const val HISTORY_PAGE_SIZE = 30

        /** 会话列表一页 20 条（后端 pageSize 上限 50） */
        const val SESSION_PAGE_SIZE = 20

        /** 表格消息后端只存前 20 行 */
        const val TABLE_PREVIEW_ROWS = 20

        /** 长期记忆单条上限（超出后端截断） */
        const val MEMORY_MAX_LENGTH = 500

        /** 「把 X 做成一份 PPT」里的动宾尾巴，生成主题时要剥掉 */
        val PPT_SUFFIX = Regex(
            "(做成|生成|制作|做|来)(一份|一个|一页|几页)?\\s*(PPT|ppt|幻灯片|课件).*$"
        )

        /** 主题前的客套话 */
        val PPT_PREFIX = Regex("^(帮我|请|麻烦|我想|想要)?\\s*(把|将)?\\s*")
    }
}
