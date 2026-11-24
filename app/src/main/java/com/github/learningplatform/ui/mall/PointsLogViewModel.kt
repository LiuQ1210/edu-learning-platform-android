package com.github.learningplatform.ui.mall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.PointsLogDto
import com.github.learningplatform.data.repository.CheckinRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PointsLogUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val logs: List<PointsLogDto> = emptyList(),
    val total: Long = 0,
    val pageNum: Int = 1,
    /** null = 全部来源 */
    val filterType: String? = null
) {
    val hasMore: Boolean get() = logs.size.toLong() < total
}

/**
 * 积分明细（后端 `GET /api/v1/points/logs`）。
 *
 * 切筛选一次性重置列表；上拉/点「加载更多」按 pageNum+1 追加，不改筛选条件。
 * 失败时保留已加载的列表，只把错误提示放到 state 里。
 */
@HiltViewModel
class PointsLogViewModel @Inject constructor(
    private val checkinRepository: CheckinRepository
) : ViewModel() {

    private val pageSize = 20

    private val _uiState = MutableStateFlow(PointsLogUiState())
    val uiState: StateFlow<PointsLogUiState> = _uiState.asStateFlow()

    init { load(reset = true) }

    fun setFilter(type: String?) {
        if (_uiState.value.filterType == type) return
        _uiState.value = _uiState.value.copy(filterType = type)
        load(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        load(reset = false)
    }

    fun load(reset: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val page = if (reset) 1 else state.pageNum + 1
            _uiState.value = if (reset) {
                state.copy(isLoading = true, isLoadingMore = false, error = null)
            } else {
                state.copy(isLoadingMore = true, error = null)
            }
            try {
                val data = checkinRepository.getPointsLogs(page, pageSize, state.filterType)
                val merged = if (reset) data.list else state.logs + data.list
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    logs = merged,
                    total = data.total,
                    pageNum = page
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.readableMessage()
                )
            }
        }
    }
}
