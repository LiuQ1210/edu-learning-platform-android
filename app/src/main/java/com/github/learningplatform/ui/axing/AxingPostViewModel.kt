package com.github.learningplatform.ui.axing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.repository.AgentRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AxingPostUiState(
    val messages: List<AxingPostMessage> = emptyList(),
    val input: String = "",
    val thinking: Boolean = false,
    val sessionId: Long = 0
)

/**
 * 帖子内「问阿星」。
 *
 * 后端 `/agent/community/ask` 只能按问题检索帖子、不能按 postId 限定范围，
 * 所以首轮把标题拼进问题里，后续轮次靠 sessionId 携带上下文。
 */
@HiltViewModel
class AxingPostViewModel @Inject constructor(
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AxingPostUiState())
    val state: StateFlow<AxingPostUiState> = _state.asStateFlow()

    fun setInput(value: String) {
        _state.value = _state.value.copy(input = value)
    }

    fun ask(articleTitle: String, text: String) {
        val question = text.trim()
        if (question.isEmpty() || _state.value.thinking) return
        _state.value = _state.value.copy(
            messages = _state.value.messages + AxingPostMessage.User(question),
            input = "",
            thinking = true
        )
        send(articleTitle, question)
    }

    /** 失败后重试上一条问题（不重复追加用户气泡） */
    fun retry(articleTitle: String) {
        if (_state.value.thinking) return
        val lastUser = _state.value.messages.lastOrNull { it is AxingPostMessage.User }
            as? AxingPostMessage.User ?: return
        _state.value = _state.value.copy(
            messages = _state.value.messages.dropLastWhile { it is AxingPostMessage.Error },
            thinking = true
        )
        send(articleTitle, lastUser.text)
    }

    private fun send(articleTitle: String, text: String) {
        viewModelScope.launch {
            try {
                val sessionId = _state.value.sessionId
                val question = if (sessionId > 0) {
                    text
                } else {
                    "我正在看社区帖子《$articleTitle》。$text"
                }
                val result = agentRepository.communityAsk(
                    question = question,
                    sessionId = sessionId.takeIf { it > 0 }
                )
                val answer = AxingPostAnswer(
                    paragraphs = paragraphsOf(result.answer),
                    quotes = result.refs.map {
                        AxingQuote(
                            title = it.postTitle.ifBlank { "相关帖子" },
                            excerpt = it.category.ifBlank { "社区帖子" }
                        )
                    }
                )
                if (answer.paragraphs.isEmpty()) {
                    throw ApiException(-1, "阿星没有返回内容，请换个问法再试")
                }
                _state.value = _state.value.copy(
                    thinking = false,
                    sessionId = result.sessionId.takeIf { it > 0 } ?: sessionId,
                    messages = _state.value.messages + AxingPostMessage.Answer(answer)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = _state.value.copy(
                    thinking = false,
                    messages = _state.value.messages + AxingPostMessage.Error(e.readableMessage())
                )
            }
        }
    }

    private fun paragraphsOf(text: String): List<String> =
        text.split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
