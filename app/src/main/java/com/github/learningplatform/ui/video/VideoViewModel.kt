package com.github.learningplatform.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.data.remote.dto.CommentDto
import com.github.learningplatform.data.remote.dto.CreateCommentRequest
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
 * 单个视频的点赞/收藏状态与计数。
 *
 * 单独放到独立 StateFlow：点赞/收藏只更新这一张小表，操作栏子组件按 courseId 订阅，
 * 不再改写主 [VideoUiState]，避免触发根组件与 Pager 内多页 VideoPage（含播放器）重组卡顿。
 */
data class InteractionMeta(
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val likeCount: Int = 0,
    val favoriteCount: Int = 0
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
    private val interactionRepository: InteractionRepository,
    private val videoApi: com.github.learningplatform.data.remote.VideoApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoUiState())
    val uiState: StateFlow<VideoUiState> = _uiState.asStateFlow()

    /** courseId -> 点赞/收藏状态与计数；独立于 [uiState]，点赞收藏不刷新主列表状态 */
    private val _interaction = MutableStateFlow<Map<Long, InteractionMeta>>(emptyMap())
    val interaction: StateFlow<Map<Long, InteractionMeta>> = _interaction.asStateFlow()

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
                // 调用短视频接口
                val resp = videoApi.getVideoList(pageNum = nextPage, pageSize = 10)
                if (resp.isSuccessful && resp.body() != null) {
                    val page = resp.body()!!
                    // 把 ShortVideoDto 映射成 CourseDto，复用现有 UI
                    val videoList = page.rows.map { shortVideo ->
                        CourseDto(
                            courseId = shortVideo.videoId,
                            courseName = shortVideo.videoTitle,
                            title = shortVideo.videoTitle,
                            coverUrl = com.github.learningplatform.core.network.ImageUrl.resolve(shortVideo.coverUrl),
                            intro = shortVideo.intro,
                            likeCount = shortVideo.likeCount,
                            favoriteCount = shortVideo.favoriteCount,
                            commentCount = shortVideo.commentCount,
                            viewCount = shortVideo.viewCount,
                            duration = shortVideo.duration
                        )
                    }
                    // 短视频的 videoUrl 直接就是播放地址，拼接完整 URL 后存到 playUrls
                    val newPlayUrls = page.rows.associate {
                        it.videoId to com.github.learningplatform.core.network.ImageUrl.resolve(it.videoUrl)
                    }
                    val accumulated = if (reset) videoList else snapshot.videos + videoList
                    // 基准点赞/收藏计数进独立互动表：首屏重置，翻页合并；是否已赞/藏由后续 toggle 结果回写
                    val newMeta = videoList.associate {
                        it.courseId to InteractionMeta(
                            likeCount = it.likeCount,
                            favoriteCount = it.favoriteCount
                        )
                    }
                    _interaction.value = if (reset) newMeta else _interaction.value + newMeta
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = null,
                        videos = accumulated,
                        playUrls = _uiState.value.playUrls + newPlayUrls,
                        pageNum = nextPage,
                        hasMore = accumulated.size < page.total
                    )
                } else {
                    throw Exception("接口返回失败")
                }
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

    // 同一目标点赞/收藏请求进行中集合：请求未返回前忽略重复点击。
    // 否则多个协程会基于同一份旧快照并发叠加，网络返回顺序交错后，
    // 图标亮灭与计数会互相打架、数字漂移。
    private val likePending = mutableSetOf<Long>()
    private val favPending = mutableSetOf<Long>()

    fun toggleLike(courseId: Long) {
        // 已有一次点赞请求在飞，直接忽略本次连点，不重复发请求
        if (!likePending.add(courseId)) return
        // 点击前快照，失败时据此回滚
        val before = _interaction.value[courseId] ?: InteractionMeta()
        // 乐观更新：先在本地翻转图标与计数，UI 毫秒级响应，不再干等后端往返；
        // 后端链路慢（远程库串行多条 SQL），等回包才亮按钮会明显卡顿。
        val optimisticActive = !before.isLiked
        val optimisticCount = (before.likeCount + if (optimisticActive) 1 else -1).coerceAtLeast(0)
        _interaction.value = _interaction.value + (courseId to before.copy(
            isLiked = optimisticActive,
            likeCount = optimisticCount
        ))
        viewModelScope.launch {
            try {
                val result = interactionRepository.toggleLike(Constants.TARGET_SHORT_VIDEO, courseId)
                // 回包后以后端权威状态与计数覆盖，纠正乐观估算可能的偏差
                val current = _interaction.value[courseId] ?: InteractionMeta()
                _interaction.value = _interaction.value + (courseId to current.copy(
                    isLiked = result.isLiked,
                    likeCount = result.likeCount
                ))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // 请求失败：回滚到点击前状态，图标与计数都不保留乐观结果
                _interaction.value = _interaction.value + (courseId to before)
            } finally {
                likePending.remove(courseId)
            }
        }
    }

    fun toggleFavorite(courseId: Long) {
        // 已有一次收藏请求在飞，直接忽略本次连点，不重复发请求
        if (!favPending.add(courseId)) return
        val before = _interaction.value[courseId] ?: InteractionMeta()
        val optimisticActive = !before.isFavorited
        val optimisticCount = (before.favoriteCount + if (optimisticActive) 1 else -1).coerceAtLeast(0)
        _interaction.value = _interaction.value + (courseId to before.copy(
            isFavorited = optimisticActive,
            favoriteCount = optimisticCount
        ))
        viewModelScope.launch {
            try {
                val result = interactionRepository.toggleFavorite(Constants.TARGET_SHORT_VIDEO, courseId)
                val current = _interaction.value[courseId] ?: InteractionMeta()
                _interaction.value = _interaction.value + (courseId to current.copy(
                    isFavorited = result.isFavorited,
                    favoriteCount = result.favoriteCount
                ))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                // 请求失败：回滚到点击前状态
                _interaction.value = _interaction.value + (courseId to before)
            } finally {
                favPending.remove(courseId)
            }
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
                    Constants.TARGET_SHORT_VIDEO,
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

    /**
     * 发评论：调后端 `POST /api/v1/comment/add`，成功后重新拉列表并给评论数 +1。
     *
     * 之前的实现只在本地列表插一条假评论（userName="我"），既没落库也不会让
     * `course.commentCount` 变化 —— 表现为"发了评论但刷新就没了、评论数不增加"。
     *
     * 注意 targetType 用数字（Constants.TARGET_COURSE=2），后端评论接口只认数字。
     */
    fun postComment(text: String, parentId: Long = 0L) {
        val target = _uiState.value.commentsTarget ?: return
        val content = text.trim()
        if (content.isEmpty()) return

        viewModelScope.launch {
            try {
                interactionRepository.createComment(
                    CreateCommentRequest(
                        targetType = Constants.TARGET_SHORT_VIDEO,
                        targetId = target,
                        rootId = 0,
                        // parentId > 0 才是「回复某条评论」，后端会把这条挂到该评论的 children 下
                        parentId = parentId,
                        content = content
                    )
                )
                // 评论数 +1：右侧操作栏的评论数取自 CourseDto.commentCount
                _uiState.value = _uiState.value.copy(
                    message = "评论成功",
                    videos = _uiState.value.videos.map {
                        if (it.courseId == target) {
                            it.copy(commentCount = it.commentCount + 1)
                        } else {
                            it
                        }
                    }
                )
                // 重新拉一遍，用后端返回的正式数据（评论 ID / 头像 / 时间）替换本地态
                loadComments(target, reset = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
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
                    targetType = Constants.TARGET_SHORT_VIDEO,
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