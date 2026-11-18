package com.github.learningplatform.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.repository.InteractionRepository
import com.github.learningplatform.data.repository.NotificationRepository
import com.github.learningplatform.ui.PagedUiState
import com.github.learningplatform.ui.isStaleRequest
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    /** 评论/回复通知关联的评论 id（后端补字段后下发；0 表示后端未返回） */
    val commentId: Long = 0L,
    val fromUserId: Long = 0L,
    val fromUserName: String = "",
    val fromUserAvatar: String = ""
)

/** 通知点击后下发的导航指令：targetType 1文章 2课程 3短视频 */
data class NotificationNav(
    val targetType: String,
    val targetId: Long,
    val commentId: Long? = null,
    val rootId: Long = 0L,
    val pageNum: Int = 1
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
    private val notificationRepository: NotificationRepository,
    private val interactionRepository: InteractionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    /** 一次性导航事件：由 Screen 收集后调用对应跳转回调 */
    private val _navEvent = MutableSharedFlow<NotificationNav>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<NotificationNav> = _navEvent.asSharedFlow()

    /** 页面提示（如评论已删除），由 Snackbar 展示 */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() { _message.value = null }

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

    // ---- 点击跳转 ----

    /**
     * 通知内容区点击。关注类无具体目标不跳转；评论/回复先调 locate 校验归属与删除状态，
     * 再下发导航指令；点赞/收藏按通知表字段直接跳转。
     */
    fun onNotificationClick(item: NotificationUiItem) {
        when (item.category) {
            "follow" -> return
            "comment" -> handleCommentClick(item)
            else -> emitNav(item.targetType, item.targetId, commentId = null)
        }
    }

    private fun handleCommentClick(item: NotificationUiItem) {
        val commentId = item.commentId.takeIf { it != 0L }
        // 老通知未带 commentId：无法定位，回退用通知表自带字段跳转
        if (commentId == null) {
            emitNav(item.targetType, item.targetId, commentId = null)
            return
        }
        viewModelScope.launch {
            val locate = interactionRepository.locateComment(commentId)
            when {
                // 定位失败：不阻塞跳转，回退到通知表字段
                locate == null -> emitNav(item.targetType, item.targetId, commentId)
                // 评论已软删：提示且不跳转
                locate.deleted -> _message.value = "评论已删除"
                // 以后端权威归属纠偏 targetType/targetId，并透传 rootId/pageNum 供定位高亮
                else -> emitNav(
                    targetType = locate.targetType,
                    targetId = locate.targetId,
                    commentId = commentId,
                    rootId = locate.rootId,
                    pageNum = locate.pageNum
                )
            }
        }
    }

    private fun emitNav(
        targetType: String,
        targetId: Long,
        commentId: Long?,
        rootId: Long = 0L,
        pageNum: Int = 1
    ) {
        if (targetType.isBlank() || targetId == 0L) return
        _navEvent.tryEmit(NotificationNav(targetType, targetId, commentId, rootId, pageNum))
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
                        val targetTypeRaw = row["targetType"] as? String ?: ""
                        // 短视频由后台上传，点赞/收藏只统计在后台，不在通知页展示；
                        // 但视频评论区的评论/回复通知保留，点击跳转到对应视频评论区并定位高亮
                        val isVideoLikeOrFav = targetTypeRaw == "3" && (notifType == "1" || notifType == "2")
                        if (isVideoLikeOrFav) return@mapNotNull null
                        // 视频由后台发布、非用户上传，文案不使用「你的视频」；互动统称「内容」，跳转目标由 targetType 分流
                        val title = when (notifType) {
                            "1" -> "${fromName} 赞了你的内容"
                            "2" -> "${fromName} 收藏了你的内容"
                            "3" -> "${fromName} 评论了你的内容"
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
                            commentId = (row["commentId"] as? Number)?.toLong() ?: 0L,
                            fromUserId = (row["fromUserId"] as? Number)?.toLong() ?: 0L,
                            fromUserName = fromName,
                            fromUserAvatar = row["fromUserAvatar"] as? String ?: ""
                        )
                    } catch (_: Throwable) {
                        null
                    }
                }
                if (_uiState.value.isStaleRequest(generation)) return@launch
                // 后端分页当前未生效：第二页会重复返回第一页全量，这里按通知 id 去重兜底，
                // 避免翻页追加后出现重复条目。
                val accumulated = (if (reset) items else snapshot.items + items)
                    .distinctBy { it.id }
                // 追加后条数没有增长，说明后端已无更多数据（或重复返回），停止继续翻页
                val noNew = !reset && accumulated.size <= snapshot.items.size
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    items = accumulated,
                    pageNum = nextPage,
                    hasMore = !noNew && accumulated.size < nextPage * 20
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
