package com.github.learningplatform.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.ArticleDto
import com.github.learningplatform.data.repository.ArticleRepository
import com.github.learningplatform.data.repository.CategoryRepository
import com.github.learningplatform.ui.course.CATEGORY_TYPE_ARTICLE
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

data class CommunityUiState(

    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val categories: List<Pair<Long?, String>> = emptyList(),
    val selectedCategoryId: Long? = null,
    val sort: Int = 1,
    val articles: List<ArticleDto> = emptyList(),
    val pageNum: Int = 1,
    val hasMore: Boolean = false,
    override val generation: Int = 0
) : PagedUiState

/**
 * 社区文章流。
 *
 * 分类与排序任一变化都重新从第 1 页拉取（服务端分页，客户端只累加）。
 */
@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityUiState())
    val uiState: StateFlow<CommunityUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        load(reset = true)
    }

    fun refresh() = load(reset = true)

    fun selectCategory(categoryId: Long?) {
        if (categoryId == _uiState.value.selectedCategoryId) return
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
        load(reset = true)
    }

    fun selectSort(sort: Int) {
        if (sort == _uiState.value.sort) return
        _uiState.value = _uiState.value.copy(sort = sort)
        load(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || !state.hasMore) return
        load(reset = false)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val tree = categoryRepository.getTree(CATEGORY_TYPE_ARTICLE)
                _uiState.value = _uiState.value.copy(
                    categories = tree.map { it.categoryId to it.categoryName }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // 分类失败不阻塞文章流
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
                val page = articleRepository.getArticles(
                    pageNum = nextPage,
                    pageSize = 20,
                    categoryId = snapshot.selectedCategoryId,
                    sort = snapshot.sort
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
