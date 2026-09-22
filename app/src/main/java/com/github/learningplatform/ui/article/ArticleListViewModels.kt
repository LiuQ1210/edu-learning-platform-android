package com.github.learningplatform.ui.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.ArticleDto
import com.github.learningplatform.data.repository.ArticleRepository
import com.github.learningplatform.ui.readableMessage
import com.github.learningplatform.ui.isStaleRequest
import com.github.learningplatform.ui.PagedUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyArticlesUiState(

    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val status: Int? = null,
    val articles: List<ArticleDto> = emptyList(),
    val pageNum: Int = 1,
    val hasMore: Boolean = false,
    val message: String? = null,
    override val generation: Int = 0
) : PagedUiState

/** 我的发布（接口 7.6），支持按状态筛选与 7.5 删除、7.7 撤回 */
@HiltViewModel
class MyArticlesViewModel @Inject constructor(
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyArticlesUiState())
    val uiState: StateFlow<MyArticlesUiState> = _uiState.asStateFlow()

    init { load(reset = true) }

    fun refresh() = load(reset = true)

    fun selectStatus(status: Int?) {
        if (status == _uiState.value.status) return
        _uiState.value = _uiState.value.copy(status = status)
        load(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || !state.hasMore) return
        load(reset = false)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun delete(articleId: Long) {
        viewModelScope.launch {
            try {
                articleRepository.deleteArticle(articleId)
                _uiState.value = _uiState.value.copy(
                    articles = _uiState.value.articles.filterNot { it.articleId == articleId },
                    message = "已删除"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    private fun load(reset: Boolean) {
        val snapshot = _uiState.value
        val nextPage = if (reset) 1 else snapshot.pageNum + 1
        // reset 时开启新一轮；飞行中的旧请求返回后因代次不匹配而被丢弃
        val generation = if (reset) snapshot.generation + 1 else snapshot.generation

        viewModelScope.launch {
            _uiState.value = snapshot.copy(
                isLoading = reset,
                isLoadingMore = !reset,
                error = if (reset) null else snapshot.error,
                generation = generation
            )
            try {
                val page = articleRepository.getMyArticles(nextPage, 20, snapshot.status)
                if (_uiState.value.isStaleRequest(generation)) return@launch
                val accumulated = if (reset) page.list else snapshot.articles + page.list
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    articles = accumulated,
                    pageNum = nextPage,
                    hasMore = accumulated.size < page.total
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (_uiState.value.isStaleRequest(generation)) return@launch
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = if (reset) e.readableMessage() else _uiState.value.error
                )
            }
        }
    }
}

data class TagArticlesUiState(

    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val tagName: String = "",
    val articles: List<ArticleDto> = emptyList(),
    val pageNum: Int = 1,
    val hasMore: Boolean = false,
    override val generation: Int = 0
) : PagedUiState

/**
 * 按标签浏览（接口 7.1 tagId）。
 *
 * 标签名需要从 7.8 标签列表里反查：7.1 的响应只带文章，不带标签名。
 */
@HiltViewModel
class TagArticlesViewModel @Inject constructor(
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagArticlesUiState())
    val uiState: StateFlow<TagArticlesUiState> = _uiState.asStateFlow()

    private var tagId: Long = 0L
    private var loaded = false

    fun load(id: Long) {
        if (loaded && id == tagId) return
        tagId = id
        loaded = true
        viewModelScope.launch {
            try {
                val name = articleRepository.getTags().firstOrNull { it.tagId == id }?.name.orEmpty()
                _uiState.value = _uiState.value.copy(tagName = name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // 标签名拿不到就只显示 ID，不影响列表
            }
        }
        loadPage(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || !state.hasMore) return
        loadPage(reset = false)
    }

    private fun loadPage(reset: Boolean) {
        val snapshot = _uiState.value
        val nextPage = if (reset) 1 else snapshot.pageNum + 1
        // reset 时开启新一轮；飞行中的旧请求返回后因代次不匹配而被丢弃
        val generation = if (reset) snapshot.generation + 1 else snapshot.generation

        viewModelScope.launch {
            _uiState.value = snapshot.copy(
                isLoading = reset,
                isLoadingMore = !reset,
                error = if (reset) null else snapshot.error,
                generation = generation
            )
            try {
                val page = articleRepository.getArticles(
                    pageNum = nextPage,
                    pageSize = 20,
                    tagId = tagId
                )
                if (_uiState.value.isStaleRequest(generation)) return@launch
                val accumulated = if (reset) page.list else snapshot.articles + page.list
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    articles = accumulated,
                    pageNum = nextPage,
                    hasMore = accumulated.size < page.total
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (_uiState.value.isStaleRequest(generation)) return@launch
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = if (reset) e.readableMessage() else _uiState.value.error
                )
            }
        }
    }
}
