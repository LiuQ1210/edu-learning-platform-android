package com.github.learningplatform.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.DownloadRecordDto
import com.github.learningplatform.data.remote.dto.UserTargetItemDto
import com.github.learningplatform.data.repository.UserRepository
import com.github.learningplatform.ui.PagedUiState
import com.github.learningplatform.ui.isStaleRequest
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------------------------------------------------------------------
// 我的点赞（3.8）
//
// 与 FavoritesViewModel 结构相同，但刻意不复用同一个 ViewModel：
// 两者分页状态、空态文案、后续可能加的「取消点赞」交互都会分化，
// 合并成一个带参数的 ViewModel 会在第一次需求分叉时就要拆开。
// ---------------------------------------------------------------------------

data class LikesUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val targetType: Int? = 2,
    val items: List<UserTargetItemDto> = emptyList(),
    val pageNum: Int = 1,
    val hasMore: Boolean = false,
    override val generation: Int = 0
) : PagedUiState

@HiltViewModel
class LikesViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LikesUiState())
    val uiState: StateFlow<LikesUiState> = _uiState.asStateFlow()

    init { load(reset = true) }

    fun refresh() = load(reset = true)

    fun selectType(targetType: Int?) {
        if (targetType == _uiState.value.targetType) return
        _uiState.value = _uiState.value.copy(targetType = targetType)
        load(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || !state.hasMore) return
        load(reset = false)
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
                val page = userRepository.getLikes(nextPage, 20, snapshot.targetType)
                if (_uiState.value.isStaleRequest(generation)) return@launch
                val accumulated = if (reset) page.list else snapshot.items + page.list
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    items = accumulated,
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

// ---------------------------------------------------------------------------
// 我的下载记录（3.11）
//
// 注意：这个列表**不代表本地已下载的文件**，只是服务端的下载流水。
// 真正的离线下载（文件落盘、断点续传、存储权限）不在本阶段范围，
// 所以这里没有提供「播放本地文件」的能力，点击行为是打开对应内容页。
// ---------------------------------------------------------------------------

data class DownloadsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val resourceType: Int? = null,
    val items: List<DownloadRecordDto> = emptyList(),
    val pageNum: Int = 1,
    val hasMore: Boolean = false,
    val message: String? = null,
    override val generation: Int = 0
) : PagedUiState

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init { load(reset = true) }

    fun refresh() = load(reset = true)

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun selectType(resourceType: Int?) {
        if (resourceType == _uiState.value.resourceType) return
        _uiState.value = _uiState.value.copy(resourceType = resourceType)
        load(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || !state.hasMore) return
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        val snapshot = _uiState.value
        val nextPage = if (reset) 1 else snapshot.pageNum + 1
        val generation = if (reset) snapshot.generation + 1 else snapshot.generation

        viewModelScope.launch {
            _uiState.value = snapshot.copy(
                isLoading = reset,
                isLoadingMore = !reset,
                error = if (reset) null else snapshot.error,
                generation = generation
            )
            try {
                val page = userRepository.getDownloadRecords(nextPage, 20, snapshot.resourceType)
                if (_uiState.value.isStaleRequest(generation)) return@launch
                val accumulated = if (reset) page.list else snapshot.items + page.list
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    items = accumulated,
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
