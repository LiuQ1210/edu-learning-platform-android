package com.github.learningplatform.ui.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.CreateArticleRequest
import com.github.learningplatform.data.remote.dto.PostStatus
import com.github.learningplatform.data.repository.ArticleRepository
import com.github.learningplatform.data.repository.CategoryRepository
import com.github.learningplatform.ui.course.CATEGORY_TYPE_ARTICLE
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val MAX_TAGS = 5

data class ArticleEditUiState(
    val isEdit: Boolean = false,
    val title: String = "",
    val summary: String = "",
    val content: String = "",
    val categoryId: Long? = null,
    val categories: List<Pair<Long, String>> = emptyList(),
    val tags: List<com.github.learningplatform.data.remote.dto.ArticleTagDto> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val submitting: Boolean = false,
    val finished: Boolean = false,
    val message: String? = null
) {
    val canPublish: Boolean
        get() = title.isNotBlank() && content.isNotBlank() && categoryId != null
}

/**
 * 文章编辑。
 *
 * 正文以纯文本编辑，提交前把空行分段转成 <p> 标签，与后端 HTML 正文格式对齐。
 * 不涉及封面：发布时 coverUrl 传 null，列表/详情页的封面完全由后端下发。
 */
@HiltViewModel
class ArticleEditViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleEditUiState())
    val uiState: StateFlow<ArticleEditUiState> = _uiState.asStateFlow()

    private var articleId: Long = 0L
    private var initialized = false

    fun init(id: Long) {
        if (initialized) return
        initialized = true
        articleId = id
        _uiState.value = _uiState.value.copy(isEdit = id > 0L)

        viewModelScope.launch {
            loadCategories()
            loadTags()
            if (id > 0L) loadArticle(id)
        }
    }

    private suspend fun loadCategories() {
        try {
            val tree = categoryRepository.getTree(CATEGORY_TYPE_ARTICLE)
            val flat = tree.flatMap { parent ->
                listOf(parent.categoryId to parent.categoryName) +
                    parent.children.map { it.categoryId to it.categoryName }
            }
            _uiState.value = _uiState.value.copy(categories = flat)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // 分类拉取失败时保留空列表，UI 会给出「暂无分类」提示
        }
    }

    private suspend fun loadTags() {
        try {
            val tags = articleRepository.getTags()
            _uiState.value = _uiState.value.copy(tags = tags)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // 标签非必填
        }
    }

    private suspend fun loadArticle(id: Long) {
        try {
            val detail = articleRepository.getArticleDetail(id)
            _uiState.value = _uiState.value.copy(
                title = detail.title,
                summary = detail.summary,
                content = htmlToPlain(detail.content),
                categoryId = detail.categoryId.takeIf { it > 0L },
                selectedTagIds = detail.tags.map { it.tagId }.toSet()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            _uiState.value = _uiState.value.copy(message = e.readableMessage())
        }
    }

    fun onTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(title = value)
    }

    fun onSummaryChange(value: String) {
        _uiState.value = _uiState.value.copy(summary = value)
    }

    fun onContentChange(value: String) {
        _uiState.value = _uiState.value.copy(content = value)
    }

    fun selectCategory(id: Long) {
        _uiState.value = _uiState.value.copy(categoryId = id)
    }

    fun toggleTag(tagId: Long) {
        val current = _uiState.value.selectedTagIds
        _uiState.value = _uiState.value.copy(
            selectedTagIds = when {
                tagId in current -> current - tagId
                current.size >= MAX_TAGS -> current
                else -> current + tagId
            }
        )
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    /** @param publish 0-存草稿 1-提交发布 */
    fun submit(publish: Int) {
        val state = _uiState.value
        if (state.submitting) return
        if (state.title.isBlank()) {
            _uiState.value = state.copy(message = "标题不能为空")
            return
        }
        // categoryId 在 7.3/7.4 都是必填，草稿也不例外，否则后端直接 400
        if (state.categoryId == null) {
            _uiState.value = state.copy(message = "请选择分类")
            return
        }
        if (publish == 1 && state.content.isBlank()) {
            _uiState.value = state.copy(message = "正文不能为空")
            return
        }

        // 后端目前只有发布接口（POST /post/add），没有 post/edit，也没有草稿接口：
        //  - 编辑已有文章 → 直接提示，避免走假成功
        //  - 存草稿      → 后端不支持，提示用户直接发布
        if (articleId > 0L) {
            _uiState.value = state.copy(message = "后端暂不支持编辑文章")
            return
        }
        if (publish == 0) {
            _uiState.value = state.copy(message = "后端暂不支持保存草稿，请直接发布")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(submitting = true)
            try {
                val html = plainToHtml(state.content)
                val result = articleRepository.createArticle(
                    CreateArticleRequest(
                        title = state.title,
                        summary = state.summary.ifBlank { null },
                        content = html,
                        categoryId = state.categoryId ?: 0L,
                        tagIds = state.selectedTagIds.toList(),
                        publish = publish
                    )
                )
                _uiState.value = _uiState.value.copy(
                    submitting = false,
                    finished = true,
                    // 后端普通用户发帖统一进「待审核」，需管理员审核通过后才公开
                    message = if (result.status == PostStatus.PENDING) {
                        "提交成功，等待审核"
                    } else {
                        "已提交发布"
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    submitting = false,
                    message = e.readableMessage()
                )
            }
        }
    }

    /** 纯文本 -> 简单 HTML：空行分段 */
    private fun plainToHtml(text: String): String =
        text.split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("") { "<p>" + it.replace("<", "&lt;").replace(">", "&gt;") + "</p>" }

    /** HTML -> 纯文本：仅用于把已有正文搬进编辑器（不追求保真） */
    private fun htmlToPlain(html: String): String =
        html.replace(Regex("</p>|</div>|<br\\s*/?>"), "\n\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .trim()
}
