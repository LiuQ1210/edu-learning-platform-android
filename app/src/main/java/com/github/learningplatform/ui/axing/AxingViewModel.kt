package com.github.learningplatform.ui.axing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.remote.dto.AgentMessageDto
import com.github.learningplatform.data.remote.dto.AgentPptResultDto
import com.github.learningplatform.data.remote.dto.AgentSessionDto
import com.github.learningplatform.data.repository.AgentRepository
import com.github.learningplatform.data.repository.CourseRepository
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
    /** 后端会话 ID：第一次问答由后端创建，后续消息带上它延续上下文 */
    val sessionId: Long = 0,
    /** 当前会话场景：chat / community。社区会话不能继续用来做课程问答 */
    val sessionScene: String = "chat",
    /** 从播放页进入时带上当前小节，视频总结要用 */
    val videoId: Long = 0,
    val fullscreenDeck: AxingDeck? = null
)

@HiltViewModel
class AxingViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val courseRepository: CourseRepository,
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AxingUiState())
    val state: StateFlow<AxingUiState> = _state.asStateFlow()

    private var replyJob: Job? = null

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
        _state.value = _state.value.copy(input = value)
    }

    fun selectCapability(capability: AxingCapability) {
        _state.value = _state.value.copy(
            capability = if (_state.value.capability == capability) null else capability
        )
    }

    fun clearCapability() {
        _state.value = _state.value.copy(capability = null)
    }

    fun useSuggestion(suggestion: AxingSuggestion) {
        _state.value = _state.value.copy(
            input = suggestion.text,
            capability = suggestion.capability
        )
    }

    // ------------------------------------------------------------ 课程选择

    fun openCoursePicker() {
        _state.value = _state.value.copy(showCoursePicker = true)
        if (_state.value.courses.isEmpty()) loadCourses()
    }

    fun closeCoursePicker() {
        _state.value = _state.value.copy(showCoursePicker = false)
    }

    fun retryCourses() = loadCourses()

    private fun loadCourses() {
        if (_state.value.coursesLoading) return
        _state.value = _state.value.copy(coursesLoading = true, coursesError = null)
        viewModelScope.launch {
            try {
                val page = courseRepository.getCourses(pageNum = 1, pageSize = 20)
                _state.value = _state.value.copy(
                    coursesLoading = false,
                    courses = page.list.map {
                        AxingCourse(it.courseId, it.courseName, it.categoryName)
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = _state.value.copy(
                    coursesLoading = false,
                    coursesError = e.readableMessage()
                )
            }
        }
    }

    fun selectCourse(course: AxingCourse) {
        _state.value = _state.value.copy(course = course, showCoursePicker = false)
    }

    fun attachImage(uri: String) {
        _state.value = _state.value.copy(imageUri = uri)
    }

    fun clearImage() {
        _state.value = _state.value.copy(imageUri = null)
    }

    // ------------------------------------------------------------ 历史会话

    fun openDrawer() {
        _state.value = _state.value.copy(showDrawer = true)
        loadSessions()
    }

    fun closeDrawer() {
        _state.value = _state.value.copy(showDrawer = false)
    }

    fun retrySessions() = loadSessions()

    private fun loadSessions() {
        if (_state.value.sessionsLoading) return
        _state.value = _state.value.copy(sessionsLoading = true, sessionsError = null)
        viewModelScope.launch {
            try {
                val list = agentRepository.sessions(pageNum = 1, pageSize = 20)
                _state.value = _state.value.copy(
                    sessionsLoading = false,
                    sessions = list.map { it.toHistoryItem() }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = _state.value.copy(
                    sessionsLoading = false,
                    sessionsError = e.readableMessage()
                )
            }
        }
    }

    private fun AgentSessionDto.toHistoryItem(): AxingHistoryItem {
        val sceneLabel = if (scene == "community") "社区问答" else "课程问答"
        val time = lastMessageTime.takeIf { it.isNotBlank() }?.let { " · ${shortDate(it)}" }.orEmpty()
        return AxingHistoryItem(
            sessionId = sessionId,
            title = title.ifBlank { "未命名对话" },
            meta = sceneLabel + time,
            scene = scene
        )
    }

    private fun shortDate(value: String): String =
        if (value.length >= 16) value.substring(5, 16) else value

    fun newChat() {
        replyJob?.cancel()
        _state.value = _state.value.copy(
            messages = emptyList(),
            input = "",
            capability = null,
            imageUri = null,
            isThinking = false,
            showDrawer = false,
            sessionId = 0,
            sessionScene = "chat"
        )
    }

    /** 打开历史会话：按 sessionId 拉真实消息 */
    fun openHistory(item: AxingHistoryItem) {
        replyJob?.cancel()
        _state.value = _state.value.copy(
            messages = emptyList(),
            input = "",
            imageUri = null,
            capability = null,
            showDrawer = false,
            sessionId = item.sessionId,
            sessionScene = item.scene,
            isThinking = true,
            thinkingLabel = "正在载入历史对话…"
        )
        replyJob = viewModelScope.launch {
            try {
                val list = agentRepository.messages(item.sessionId)
                _state.value = _state.value.copy(
                    isThinking = false,
                    messages = list.mapNotNull { it.toUiMessage(item.scene) }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = _state.value.copy(
                    isThinking = false,
                    messages = listOf(AxingMessage.Error(e.readableMessage()))
                )
            }
        }
    }

    private fun AgentMessageDto.toUiMessage(scene: String): AxingMessage? = when (role) {
        "user" -> AxingMessage.User(content)
        "assistant" -> {
            val blocks = buildList {
                if (content.isNotBlank()) add(AxingBlock.Paragraph(content))
                if (refs.isNotEmpty()) {
                    if (scene == "community") {
                        add(
                            AxingBlock.Quotes(
                                refs.map {
                                    AxingQuote(
                                        title = it.postTitle.ifBlank { "相关帖子" },
                                        excerpt = it.category.ifBlank { "社区帖子" }
                                    )
                                }
                            )
                        )
                    } else {
                        add(
                            AxingBlock.Sources(
                                refs.map {
                                    AxingSource(
                                        chapter = it.chapter.ifBlank { "相关章节" },
                                        title = it.course.ifBlank { "课程" }
                                    )
                                }
                            )
                        )
                    }
                }
            }
            AxingMessage.Assistant(
                AxingAnswer(
                    capability = inferCapability(content),
                    course = _state.value.course.takeIf { scene != "community" },
                    heading = null,
                    blocks = blocks
                )
            )
        }
        else -> null
    }

    // ------------------------------------------------------------ 发送

    fun send() {
        val current = _state.value
        if (current.isThinking) return
        val imageUri = current.imageUri
        val text = current.input.trim().ifBlank {
            when {
                imageUri != null -> "看看这张图片"
                current.capability != null -> "帮我${current.capability.label}"
                else -> return
            }
        }
        val capability = current.capability ?: inferCapability(text)
        _state.value = current.copy(
            messages = current.messages + AxingMessage.User(text, imageUri),
            input = "",
            imageUri = null,
            capability = null,
            isThinking = true,
            thinkingLabel = thinkingLabelOf(capability)
        )
        dispatch(text, capability, hasImage = imageUri != null)
    }

    fun regenerate() {
        if (_state.value.isThinking) return
        val messages = _state.value.messages
        val lastUser = messages.lastOrNull { it is AxingMessage.User } as? AxingMessage.User ?: return
        val capability =
            (messages.lastOrNull { it is AxingMessage.Assistant } as? AxingMessage.Assistant)
                ?.answer?.capability ?: inferCapability(lastUser.text)
        replyJob?.cancel()
        _state.value = _state.value.copy(
            messages = messages.dropLastWhile {
                it is AxingMessage.Assistant || it is AxingMessage.Error
            },
            isThinking = true,
            thinkingLabel = thinkingLabelOf(capability)
        )
        dispatch(lastUser.text, capability, hasImage = lastUser.imageUri != null)
    }

    /** 回答卡上的「做成 PPT」：用当前课程生成，不需要用户再输入 */
    fun makePpt() {
        if (_state.value.isThinking) return
        val course = _state.value.course
        val text = course?.let { "把《${it.title}》做成一份 PPT" } ?: "把课程内容做成一份 PPT"
        _state.value = _state.value.copy(
            messages = _state.value.messages + AxingMessage.User(text),
            capability = null,
            isThinking = true,
            thinkingLabel = thinkingLabelOf(AxingCapability.PPT)
        )
        launchAnswer(AxingCapability.PPT) { answerPpt() }
    }

    fun openDeck(deck: AxingDeck) {
        _state.value = _state.value.copy(fullscreenDeck = deck)
    }

    fun closeDeck() {
        _state.value = _state.value.copy(fullscreenDeck = null)
    }

    private fun dispatch(text: String, capability: AxingCapability, hasImage: Boolean) {
        if (hasImage) {
            // 后端没有视觉接口：保留入口，明确告知，不把图片发出去
            pushAnswer(
                AxingAnswer(
                    capability = AxingCapability.QA,
                    course = null,
                    heading = null,
                    blocks = listOf(
                        AxingBlock.Paragraph(
                            "图片理解暂未接入后端，这次没有把图片发出去。" +
                                "可以先用文字描述图片里的页面或题目，我再帮你分析。"
                        )
                    ),
                    caption = "图片入口先保留，等视觉模型接入后再开放。"
                )
            )
            return
        }
        launchAnswer(capability) {
            when (capability) {
                AxingCapability.STUDY -> answerStudy()
                AxingCapability.VIDEO -> answerVideo()
                AxingCapability.PPT -> answerPpt()
                AxingCapability.SUMMARY, AxingCapability.QA -> answerChat(text, capability)
            }
        }
    }

    private fun launchAnswer(capability: AxingCapability, block: suspend () -> Unit) {
        replyJob?.cancel()
        replyJob = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = _state.value.copy(
                    isThinking = false,
                    messages = _state.value.messages + AxingMessage.Error(e.readableMessage())
                )
            }
        }
    }

    private fun pushAnswer(answer: AxingAnswer) {
        _state.value = _state.value.copy(
            isThinking = false,
            messages = _state.value.messages + AxingMessage.Assistant(answer)
        )
    }

    private fun thinkingLabelOf(capability: AxingCapability): String = when (capability) {
        AxingCapability.VIDEO -> "正在分析视频…"
        AxingCapability.PPT -> "正在生成幻灯片…"
        AxingCapability.STUDY -> "正在统计学习数据…"
        AxingCapability.SUMMARY, AxingCapability.QA -> "阿星正在整理回答…"
    }

    // ------------------------------------------------------------ 各能力实现

    private suspend fun answerChat(text: String, capability: AxingCapability) {
        val course = _state.value.course
        // 社区会话不能当作课程会话续用，否则两套问答会串到同一个 session 里
        val pendingSessionId = _state.value.sessionId
            .takeIf { it > 0 && _state.value.sessionScene == "chat" }
        val result = agentRepository.chat(
            question = text,
            courseId = course?.id?.takeIf { it > 0 },
            sessionId = pendingSessionId
        )
        val blocks = buildList {
            if (result.answer.isNotBlank()) add(AxingBlock.Paragraph(result.answer))
            if (result.refs.isNotEmpty()) {
                add(
                    AxingBlock.Sources(
                        result.refs.map {
                            AxingSource(
                                chapter = it.chapter.ifBlank { "相关章节" },
                                title = it.course.ifBlank { "课程" }
                            )
                        }
                    )
                )
            }
        }
        if (result.sessionId > 0) {
            _state.value = _state.value.copy(sessionId = result.sessionId, sessionScene = "chat")
        }
        pushAnswer(
            AxingAnswer(
                capability = capability,
                course = course,
                heading = null,
                blocks = blocks,
                caption = if (result.refs.isEmpty()) null else "来源：课程知识库检索"
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
            stats.todayCourseMinutes?.let { add("今日课程 ${it} 分钟。") }
            stats.todayArticleMinutes?.let { add("今日阅读文章 ${it} 分钟。") }
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
        val taskId = agentRepository.startVideoSummary(videoId)
        val task = agentRepository.awaitTask(taskId) { t ->
            _state.value = _state.value.copy(thinkingLabel = progressLabel("正在分析视频", t.progress))
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
        pushAnswer(
            AxingAnswer(
                capability = AxingCapability.VIDEO,
                course = _state.value.course,
                heading = heading,
                blocks = listOf(
                    AxingBlock.Paragraph(
                        summary.summary.ifBlank { "这一节还没有可用的转写内容。" }
                    )
                ),
                caption = if (summary.fallback) {
                    "没有可用的字幕，以上内容基于课程信息生成。"
                } else {
                    "由视频转写内容生成"
                }
            )
        )
    }

    private suspend fun answerPpt() {
        val course = _state.value.course
        val topic = course?.title?.takeIf { it.isNotBlank() }?.let { "《$it》课程汇报" }
            ?: "课程学习总结"
        val taskId = agentRepository.startPpt(topic)
        val task = agentRepository.awaitTask(taskId) { t ->
            _state.value = _state.value.copy(thinkingLabel = progressLabel("正在生成幻灯片", t.progress))
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
                    if (deck.fileUrl.isNotBlank()) append("，导出可下载 PPTX 文件。")
                    else append("。")
                }
            )
        )
    }

    private fun AgentPptResultDto.toDeck(course: AxingCourse?): AxingDeck {
        val outline = outline
        val fallbackTitle = course?.title?.let { "《$it》课程汇报" } ?: "课程汇报"

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
            title = outline.title.ifBlank { fallbackTitle },
            subtitle = outline.subtitle,
            bullets = emptyList(),
            footer = "共 ${bodySlides.size + 1} 页"
        )
        return AxingDeck(
            title = outline.title.ifBlank { fallbackTitle },
            courseTitle = course?.title.orEmpty(),
            slides = listOf(cover) + bodySlides,
            fileUrl = url,
            fileName = fileName
        )
    }

    private fun progressLabel(prefix: String, progress: Int): String =
        if (progress > 0) "$prefix $progress%" else "$prefix…"

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
}
