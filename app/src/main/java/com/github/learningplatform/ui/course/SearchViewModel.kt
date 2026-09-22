package com.github.learningplatform.ui.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.ContentItemDto
import com.github.learningplatform.data.repository.SearchRepository
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

data class SearchUiState(

    val keyword: String = "",
    val type: Int = 2,
    val categoryId: Long? = null,
    val hotWords: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false,
    val results: List<ContentItemDto> = emptyList(),
    val total: Long = 0L,
    val pageNum: Int = 1,
    val hasMore: Boolean = false,
    override val generation: Int = 0
) : PagedUiState

/**
 * 搜索（接口文档 5.1 / 5.2）。
 *
 * type: 1-文章 2-视频课程。切换类型会清空结果并重新查询。
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var initialized = false

    fun init(categoryId: Long?, keyword: String, initialType: Int = 2) {
        if (initialized) return
        initialized = true
        _uiState.value = _uiState.value.copy(
            categoryId = categoryId,
            keyword = keyword,
            // 调用方指定的类型优先（从社区进来是 1-文章）；
            // 带分类进入时固定看视频课程，因为分类页列的是课程
            type = if (categoryId != null) 2 else initialType
        )
        loadHotWords()
        if (keyword.isNotBlank()) search()
    }

    fun onKeywordChange(value: String) {
        _uiState.value = _uiState.value.copy(
            keyword = value,
            // 清空输入后回到热词页
            searched = if (value.isBlank()) false else _uiState.value.searched
        )
    }

    fun switchType(type: Int) {
        if (type == _uiState.value.type) return
        _uiState.value = _uiState.value.copy(type = type, searched = false, results = emptyList())
        if (_uiState.value.keyword.isNotBlank()) search()
    }

    fun search() {
        val keyword = _uiState.value.keyword.trim()
        if (keyword.isBlank() && _uiState.value.categoryId == null) return
        load(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        load(reset = false)
    }

    private fun loadHotWords() {
        viewModelScope.launch {
            try {
                val words = searchRepository.getHotWords()
                _uiState.value = _uiState.value.copy(hotWords = words.map { it.keyword })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // 热词失败不阻塞搜索
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
                val page = searchRepository.searchPage(
                    keyword = snapshot.keyword.trim().ifBlank { null },
                    type = snapshot.type,
                    categoryId = snapshot.categoryId,
                    pageNum = nextPage
                )
                if (_uiState.value.isStaleRequest(generation)) return@launch
                val accumulated = if (reset) page.list else snapshot.results + page.list
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    searched = true,
                    results = accumulated,
                    total = page.total,
                    pageNum = nextPage,
                    hasMore = accumulated.size < page.total
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    searched = true,
                    error = if (reset) e.readableMessage() else _uiState.value.error
                )
            }
        }
    }
}
