package com.github.learningplatform.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.data.remote.dto.CommentDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.repository.CourseRepository
import com.github.learningplatform.data.repository.InteractionRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 短视频类型（接口 6.1 videoType） */
private const val VIDEO_TYPE_SHORT = 1

/** 分享平台：4-复制链接（接口 8.11） */
private const val SHARE_PLATFORM_COPY_LINK = 4

data class VideoUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val videos: List<CourseDto> = emptyList(),
    /** courseId -> 限时签名播放地址（接口 6.3） */
    val playUrls: Map<Long, String> = emptyMap(),
    val likedIds: Set<Long> = emptySet(),
    val favoritedIds: Set<Long> = emptySet(),
    /** 本地点赞偏移：toggle 后 +1/-1，实时更新数字 */
    val likeDelta: Map<Long, Int> = emptyMap(),
    val favDelta: Map<Long, Int> = emptyMap(),
    val shareDelta: Map<Long, Int> = emptyMap(),
    val pageNum: Int = 1,
    val hasMore: Boolean = false,
    val commentsTarget: Long? = null,
    val commentsLoading: Boolean = false,
    val commentsError: String? = null,
    val comments: List<CommentDto> = emptyList(),
    val commentsCursor: String? = null,
    val commentsHasMore: Boolean = false,
    val message: String? = null
)

/**
 * 短视频流。
 *
 * 播放凭证按需拉取：滑到某一页才请求 6.3，避免一次请求 20 条签名 URL
 * （每个 URL 都有有效期，提前取会白白过期）。
 */
@HiltViewModel
class VideoViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val interactionRepository: InteractionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoUiState())
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    private var currentPage = -1

    init { load() }

    fun refresh() {
        _uiState.value = VideoUiState()
        load()
    }

    fun load() {
        if (_uiState.value.isLoading) return
        fetch(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || !state.hasMore) return
        fetch(reset = false)
    }

    /** 页码变化时预取当前页与下一页的播放地址 */
    fun onPageChanged(page: Int) {
        if (page == currentPage) return
        currentPage = page
        val state = _uiState.value
        listOfNotNull(state.videos.getOrNull(page), state.videos.getOrNull(page + 1))
            .forEach { ensurePlayUrl(it.courseId) }
    }

    private fun ensurePlayUrl(courseId: Long) {
        if (_uiState.value.playUrls.containsKey(courseId)) return
        viewModelScope.launch {
            try {
                val info = courseRepository.getPlayInfo(courseId, null)
                _uiState.value = _uiState.value.copy(
                    playUrls = _uiState.value.playUrls + (courseId to info.playUrl)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // 3002/3003 表示无播放权限，短视频免费内容正常不会出现；
                // 失败时保留封面，不打断滑动浏览。
            }
        }
    }

    private fun fetch(reset: Boolean) {
        val snapshot = _uiState.value
        val nextPage = if (reset) 1 else snapshot.pageNum + 1

        viewModelScope.launch {
            _uiState.value = snapshot.copy(
                isLoading = reset,
                isLoadingMore = !reset,
                error = if (reset) null else snapshot.error
            )
            try {
                val page = courseRepository.getCourses(
                    pageNum = nextPage,
                    pageSize = 10,
                    videoType = VIDEO_TYPE_SHORT,
                    sort = 2
                )
                val accumulated = if (reset) page.list else snapshot.videos + page.list
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    videos = accumulated,
                    pageNum = nextPage,
                    hasMore = accumulated.size < page.total
                )
                // 首页就把第一页的凭证取回来，进页面即可播放
                accumulated.take(2).forEach { ensurePlayUrl(it.courseId) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = if (reset) e.readableMessage() else _uiState.value.error
                )
            }
        }
    }

    fun toggleLike(courseId: Long) {
        val currentlyLiked = _uiState.value.likedIds.contains(courseId)
        val nowLiked = !currentlyLiked
        viewModelScope.launch {
            try {
                interactionRepository.toggleLike(Constants.TARGET_COURSE, courseId)
            } catch (_: Throwable) {
                // preview/离线时忽略接口异常，本地状态照常切换
            }
            val curLikes = _uiState.value.likeDelta.getOrDefault(courseId, 0)
            _uiState.value = _uiState.value.copy(
                likedIds = if (nowLiked) _uiState.value.likedIds + courseId
                else _uiState.value.likedIds - courseId,
                likeDelta = _uiState.value.likeDelta + (courseId to (if (nowLiked) curLikes + 1 else curLikes - 1))
            )
        }
    }

    fun toggleFavorite(courseId: Long) {
        val currentlyFaved = _uiState.value.favoritedIds.contains(courseId)
        val nowFaved = !currentlyFaved
        viewModelScope.launch {
            try {
                interactionRepository.toggleFavorite(Constants.TARGET_COURSE, courseId)
            } catch (_: Throwable) {
                // preview/离线时忽略接口异常，本地状态照常切换
            }
            val curFav = _uiState.value.favDelta.getOrDefault(courseId, 0)
            _uiState.value = _uiState.value.copy(
                favoritedIds = if (nowFaved) _uiState.value.favoritedIds + courseId
                else _uiState.value.favoritedIds - courseId,
                favDelta = _uiState.value.favDelta + (courseId to (if (nowFaved) curFav + 1 else curFav - 1))
            )
        }
    }

    /**
     * 分享（接口 8.11 platform=4 复制链接）。
     *
     * 复制动作在 UI 层用系统剪贴板完成；这里只负责上报转发行为，
     * 并给出提示文案。没有这一步，分享按钮点了没有任何反馈。
     */
    fun share(course: CourseDto) {
        viewModelScope.launch {
            try {
                interactionRepository.recordShare(
                    Constants.TARGET_COURSE,
                    course.courseId,
                    SHARE_PLATFORM_COPY_LINK
                )
                val curShare = _uiState.value.shareDelta.getOrDefault(course.courseId, 0)
                _uiState.value = _uiState.value.copy(
                    message = "链接已复制，快去分享吧",
                    shareDelta = _uiState.value.shareDelta + (course.courseId to curShare + 1)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun openComments(courseId: Long) {
        _uiState.value = _uiState.value.copy(commentsTarget = courseId, comments = emptyList())
        loadComments(courseId, reset = true)
    }

    fun closeComments() {
        _uiState.value = _uiState.value.copy(commentsTarget = null)
    }

    fun loadMoreComments() {
        val state = _uiState.value
        val target = state.commentsTarget ?: return
        // 必须同时判 commentsLoading：只判 hasMore 时连点两次会用同一 cursor
        // 请求两页并重复追加，LazyColumn 的 key 冲突会直接抛异常。
        if (!state.commentsHasMore || state.commentsLoading) return
        loadComments(target, reset = false)
    }

    /** 评论为游标分页（8.1），首页 cursor 传 null */
    private fun loadComments(courseId: Long, reset: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                commentsLoading = true,
                commentsError = null
            )
            try {
                val page = interactionRepository.getComments(
                    targetType = Constants.TARGET_COURSE,
                    targetId = courseId,
                    cursor = if (reset) null else _uiState.value.commentsCursor
                )
                _uiState.value = _uiState.value.copy(
                    commentsLoading = false,
                    comments = if (reset) page.list else _uiState.value.comments + page.list,
                    commentsCursor = page.nextCursor,
                    commentsHasMore = page.hasMore
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    commentsLoading = false,
                    commentsError = e.readableMessage()
                )
            }
        }
    }
}