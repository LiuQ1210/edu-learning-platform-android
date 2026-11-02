package com.github.learningplatform.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.repository.NotificationRepository
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

data class NotificationUiItem(
    val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val time: String = "",
    val read: Boolean = false,
    val category: String = "like"
)

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val selectedTab: Int = 0,
    val items: List<NotificationUiItem> = emptyList(),
    val pageNum: Int = 1,
    val hasMore: Boolean = false,
    val unreadCount: Int = 0,
    override val generation: Int = 0
) : PagedUiState

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadUnreadCount()
        load(reset = true)
    }

    fun refresh() = load(reset = true)

    fun selectTab(index: Int) {
        if (index == _uiState.value.selectedTab) return
        _uiState.value = _uiState.value.copy(selectedTab = index)
        load(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || !state.hasMore) return
        load(reset = false)
    }

    private fun loadUnreadCount() {
        viewModelScope.launch {
            try {
                val count = notificationRepository.getUnreadCount()
                _uiState.value = _uiState.value.copy(unreadCount = count)
            } catch (_: Throwable) {}
        }
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
                // 后端通知类型：1点赞 2评论 3关注 4收藏
                val typeMap = mapOf(0 to "1", 1 to "2", 2 to "3", 3 to "4")
                val type = typeMap[snapshot.selectedTab]
                // 暂时先拉全部，后面再按类型筛选
                val list = notificationRepository.getNotificationList(
                    isRead = null,
                    pageNum = nextPage,
                    pageSize = 20
                )
                val items = list.mapNotNull { map ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val row = map as? Map<String, Any?> ?: return@mapNotNull null
                        NotificationUiItem(
                            id = (row["notificationId"] as? Number)?.toLong() ?: 0L,
                            title = row["title"] as? String ?: "",
                            body = row["content"] as? String ?: "",
                            time = row["createTime"] as? String ?: "",
                            read = (row["isRead"] as? String) == "1",
                            category = when (row["type"] as? String) {
                                "1" -> "like"
                                "2" -> "comment"
                                "3" -> "follow"
                                "4" -> "favorite"
                                else -> "like"
                            }
                        )
                    } catch (_: Throwable) {
                        null
                    }
                }
                if (_uiState.value.isStaleRequest(generation)) return@launch
                val accumulated = if (reset) items else snapshot.items + items
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    items = accumulated,
                    pageNum = nextPage,
                    hasMore = accumulated.size < nextPage * 20
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
