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
    val category: String = "like",
    val targetType: String = "",
    val targetId: Long = 0L,
    val fromUserId: Long = 0L,
    val fromUserName: String = "",
    val fromUserAvatar: String = ""
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

    fun markRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                val count = notificationRepository.markRead(notificationId)
                _uiState.value = _uiState.value.copy(unreadCount = count)
                // 本地标记已读
                _uiState.value = _uiState.value.copy(
                    items = _uiState.value.items.map {
                        if (it.id == notificationId) it.copy(read = true) else it
                    }
                )
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
                // 后端通知类型：1点赞 2收藏 3评论 4关注 5回复
                val typeMap = mapOf(0 to "1", 1 to "3", 2 to "4", 3 to "2")
                val type = typeMap[snapshot.selectedTab]
                val list = notificationRepository.getNotificationList(
                    type = type,
                    isRead = null,
                    pageNum = nextPage,
                    pageSize = 20
                )
                val items = list.mapNotNull { map ->
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val row = map as? Map<String, Any?> ?: return@mapNotNull null
                        val fromName = row["fromUserName"] as? String ?: ""
                        val rawContent = row["content"] as? String ?: ""
                        val content = if (rawContent == "null") "" else rawContent
                        val notifType = row["type"] as? String ?: "1"
                        val title = when (notifType) {
                            "1" -> "${fromName} 赞了你的内容"
                            "2" -> "${fromName} 收藏了你的内容"
                            "3" -> "${fromName} 评论了你"
                            "4" -> "${fromName} 关注了你"
                            "5" -> "${fromName} 回复了你"
                            else -> fromName
                        }
                        NotificationUiItem(
                            id = (row["notificationId"] as? Number)?.toLong() ?: 0L,
                            title = title,
                            body = content,
                            time = row["createTime"] as? String ?: "",
                            read = (row["isRead"] as? String) == "1",
                            category = when (notifType) {
                                "1" -> "like"
                                "2" -> "favorite"
                                "3", "5" -> "comment"
                                "4" -> "follow"
                                else -> "like"
                            },
                            targetType = row["targetType"] as? String ?: "",
                            targetId = (row["targetId"] as? Number)?.toLong() ?: 0L,
                            fromUserId = (row["fromUserId"] as? Number)?.toLong() ?: 0L,
                            fromUserName = fromName,
                            fromUserAvatar = row["fromUserAvatar"] as? String ?: ""
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
