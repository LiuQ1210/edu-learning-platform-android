package com.github.learningplatform.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.github.learningplatform.data.remote.dto.CommentDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.ui.common.Avatar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.NetImage
import com.github.learningplatform.ui.common.formatCount
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush
import java.util.Locale

/**
 * 视频 Tab：短视频流（接口文档 6.1 videoType=1 + 6.3 播放凭证）。
 *
 * 短视频不传 lessonId，播放地址由 6.3 动态下发（限时签名 URL）。
 * 点赞/收藏复用统一互动接口（8.9 / 8.10，targetType=2）。
 */
@Composable
fun VideoScreen(
    onOpenCourse: (Long) -> Unit,
    onOpenPlayer: (Long, Long?) -> Unit,
    viewModel: VideoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 点赞/收藏/分享失败都会写 message，必须在画面上可见（黑底视频流里没有别的位置）
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(
                message = uiState.error.orEmpty(),
                onRetry = viewModel::refresh
            )
            uiState.videos.isEmpty() -> EmptyState(message = "暂无短视频")
            else -> {
                val pagerState = rememberPagerState(pageCount = { uiState.videos.size })

                // 滑到哪一页就只让那一页持有播放器，避免同时解码多个视频
                LaunchedEffect(pagerState.currentPage) {
                    viewModel.onPageChanged(pagerState.currentPage)
                }
                LaunchedEffect(pagerState.currentPage, uiState.videos.size) {
                    if (pagerState.currentPage >= uiState.videos.size - 2) viewModel.loadMore()
                }

                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val course = uiState.videos[page]
                    // 回调按 courseId 固定引用：避免每次主 state 刷新生成新 lambda、
                    // 导致相邻预加载页 VideoPage 因参数不等被迫重组。
                    val onToggleLike = remember(course.courseId) { { viewModel.toggleLike(course.courseId) } }
                    val onToggleFavorite = remember(course.courseId) { { viewModel.toggleFavorite(course.courseId) } }
                    val onOpenComments = remember(course.courseId) { { viewModel.openComments(course.courseId) } }
                    val onOpenDetail = remember(course.courseId) { { onOpenCourse(course.courseId) } }
                    VideoPage(
                        course = course,
                        isActive = page == pagerState.currentPage,
                        playUrl = uiState.playUrls[course.courseId].orEmpty(),
                        commentCount = course.commentCount,
                        viewModel = viewModel,
                        onToggleLike = onToggleLike,
                        onToggleFavorite = onToggleFavorite,
                        onOpenComments = onOpenComments,
                        onOpenDetail = onOpenDetail
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    uiState.commentsTarget?.let {
        CommentsSheet(
            loading = uiState.commentsLoading,
            comments = uiState.comments,
            error = uiState.commentsError,
            hasMore = uiState.commentsHasMore,
            onLoadMore = viewModel::loadMoreComments,
            onPostComment = viewModel::postComment,
            onDismiss = viewModel::closeComments
        )
    }
}

@Composable
private fun VideoPage(
    course: CourseDto,
    isActive: Boolean,
    playUrl: String,
    commentCount: Int,
    viewModel: VideoViewModel,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenDetail: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 未拿到播放凭证前先显示封面；拿到且处于当前页才真正起播
        if (isActive && playUrl.isNotBlank()) {
            ShortVideoPlayer(playUrl = playUrl, modifier = Modifier.fillMaxSize())
        } else {
            if (course.coverUrl.isNotBlank()) {
                NetImage(
                    url = course.coverUrl,
                    modifier = Modifier.fillMaxSize(),
                    radius = 0.dp
                )
            } else {
                // 后端未传封面（coverUrl 为空）时的兜底占位：深色背景 + 居中播放图标，
                // 避免非当前页只剩一块空白灰底；标题/时长仍由底部信息层叠加在上方。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF2B2B33), Color.Black)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            NavIcons.Play,
                            contentDescription = "封面占位",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }
            // 当前页还在等待播放地址下发（playUrl 为空）时显示转圈；
            // 进入播放器后的首帧缓冲由 ShortVideoPlayer 内部监听 STATE_BUFFERING 处理。
            if (isActive && playUrl.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        // 底部渐变，保证文字可读
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        // 右侧操作栏：点赞/收藏状态由 VideoActionBar 内部按 courseId 订阅独立互动流，
        // 点赞收藏只刷新这一栏，不牵动播放器与其他页面重组。
        VideoActionBar(
            courseId = course.courseId,
            viewModel = viewModel,
            commentCount = commentCount,
            onOpenComments = onOpenComments,
            onToggleLike = onToggleLike,
            onToggleFavorite = onToggleFavorite
        )

        // 左下信息
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 80.dp, bottom = 46.dp)
        ) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onOpenDetail)
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "时长 " + formatSeconds(course.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = formatCount(course.viewCount) + " 次播放",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * 右侧点赞/评论/收藏操作栏。
 *
 * 只订阅本 courseId 对应的互动状态：点赞/收藏回包只更新这一条 meta，
 * 本栏单独重组；父级 VideoPage、播放器、相邻页面都不参与刷新。
 */
@Composable
private fun VideoActionBar(
    courseId: Long,
    viewModel: VideoViewModel,
    commentCount: Int,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenComments: () -> Unit
) {
    val meta by remember(courseId) {
        viewModel.interaction.map { it[courseId] }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val current = meta ?: InteractionMeta()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 46.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            VideoAction(
                icon = NavIcons.Favorite,
                label = formatCount(current.likeCount),
                tint = if (current.isLiked) Color(0xFFFF4D6A) else Color.White,
                onClick = onToggleLike
            )
            VideoAction(
                icon = NavIcons.Comment,
                label = formatCount(commentCount),
                onClick = onOpenComments
            )
            VideoAction(
                icon = NavIcons.Bookmark,
                label = formatCount(current.favoriteCount),
                tint = if (current.isFavorited) Color(0xFFFFC107) else Color.White,
                onClick = onToggleFavorite
            )
        }
    }
}

@Composable
private fun VideoAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                // 视频页是黑色全屏，不能铺浅色渐变（会变成一块突兀的亮斑）。
                // 这里用半透明白，让封面透出来 —— 视频操作按钮的常规做法。
                .background(Color.White.copy(alpha = 0.18f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

/**
 * 单个短视频播放器。
 *
 * 只在所在页可见时创建，离开页面立即 release —— 短视频流里同时存活多个
 * ExoPlayer 会瞬间吃满解码器并触发 OOM。
 */
// PlayerView / setShutterBackgroundColor 标记为 @UnstableApi，
// 用 androidx.annotation.OptIn（而不是 Kotlin 的 @file:OptIn）才能真正让 lint 认可
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun ShortVideoPlayer(playUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(playUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(playUrl))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }
    var isPlaying by remember { mutableStateOf(false) }
    // 首帧缓冲中：prepare 到 STATE_READY 之间保持 true，中央显示转圈，避免黑屏像卡死
    var isBuffering by remember { mutableStateOf(true) }
    var pos by remember { mutableStateOf(0L) }
    var dur by remember { mutableStateOf(0L) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            if (player.duration > 0) dur = player.duration
            pos = player.currentPosition
            isPlaying = player.isPlaying
            kotlinx.coroutines.delay(500)
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    this.player = player
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 点击画面暂停/播放
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { if (player.isPlaying) player.pause() else player.play() }
        )

        // 首帧缓冲中：中央转圈，优先于暂停按钮，避免远程视频缓冲期间黑屏无反馈
        if (isBuffering) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // 暂停时中央显示播放按钮（缓冲中不叠加，避免两个控件重叠）
        if (!isPlaying && !isBuffering) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { player.play() },
                contentAlignment = Alignment.Center
            ) {
                Icon(NavIcons.Play, contentDescription = "播放", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        // 底部进度条
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
        ) {
            Text(fmt(pos), style = MaterialTheme.typography.labelSmall, color = Color.White)
            Slider(
                value = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f,
                onValueChange = { if (dur > 0) player.seekTo((it * dur).toLong()) },
                modifier = Modifier.weight(1f).height(24.dp),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = Color.White, activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Text(fmt(dur), style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }
}

private fun fmt(ms: Long): String {
    if (ms <= 0) return "00:00"
    val s = ms / 1000; return String.format(Locale.CHINA, "%02d:%02d", s / 60, s % 60)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CommentsSheet(
    loading: Boolean,
    comments: List<CommentDto>,
    error: String?,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onPostComment: (String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .padding(horizontal = 16.dp)
        ) {
            // 回复目标：既要知道「回复谁」（用于提示），也要知道「回复哪条评论」（parentId）
            var replyToUser by remember { mutableStateOf<String?>(null) }
            var replyToCommentId by remember { mutableStateOf<Long?>(null) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "评论",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${comments.size} 条",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint
                )
            }
            Spacer(Modifier.height(12.dp))

            when {
                loading && comments.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth()) { LoadingState() }
                error != null && comments.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth()) { ErrorState(message = error) }
                comments.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth()) { EmptyState(message = "还没有评论") }
                else -> LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(pageGradientBrush(strong = true)),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(comments, key = { it.commentId }) { comment ->
                        CommentRow(
                            comment = comment,
                            onReply = {
                                replyToCommentId = comment.commentId
                                replyToUser = comment.userName
                            }
                        )
                    }
                    if (hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "加载更多",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable(onClick = onLoadMore)
                                )
                            }
                        }
                    }
                }
            }

            // 底部评论输入框
            Spacer(Modifier.height(8.dp))
            if (replyToUser != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("回复 @$replyToUser", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        replyToUser = null
                        replyToCommentId = null
                    }) { Text("取消", color = TextHint) }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var input by remember { mutableStateOf("") }
                androidx.compose.material3.OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (replyToUser != null) "回复 @$replyToUser..." else "说点什么...", color = TextHint) },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            // 正文只存用户输入的内容；「回复谁」由 parentId + replyToName 表达，
                            // 不再把「回复 @xxx:」拼进正文（否则会变成一条普通顶级评论）
                            onPostComment(input.trim(), replyToCommentId ?: 0L)
                            input = ""
                            replyToUser = null
                            replyToCommentId = null
                        }
                    }
                ) {
                    Text("发送", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: CommentDto, onReply: () -> Unit = {}) {
    var liked by remember { mutableStateOf(comment.isLiked) }
    var likeCnt by remember { mutableStateOf(comment.likeCount) }
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp)
    ) {
        Avatar(url = comment.userAvatar, name = comment.userName, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.userName.ifBlank { "匿名用户" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(text = comment.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            var expanded by remember { mutableStateOf(false) }
            Text(
                text = if (comment.replyCount > 0) {
                    (if (expanded) "收起回复" else "查看 " + comment.replyCount + " 条回复")
                } else comment.createTime,
                style = MaterialTheme.typography.bodySmall,
                color = if (comment.replyCount > 0)     Primary else TextHint,
                modifier = if (comment.replyCount > 0) Modifier.clickable { expanded = !expanded } else Modifier
            )
            // 展开显示回复：缩进在父评论下方（抖音式），不再与顶级评论平铺在一行
            if (expanded && comment.hotReplies.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    comment.hotReplies.forEachIndexed { index, reply ->
                        if (index > 0) Spacer(Modifier.height(6.dp))
                        Text(
                            text = reply.userName + (reply.replyToName?.let { " 回复 $it" } ?: "") + "：" + reply.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        // 评论点赞 + 回复
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                NavIcons.Favorite,
                contentDescription = "赞",
                tint = if (liked) Color(0xFFFF4D6A) else TextHint,
                modifier = Modifier
                    .size(18.dp)
                    .clickable {
                        liked = !liked
                        likeCnt = if (liked) likeCnt + 1 else likeCnt - 1
                    }
            )
            Spacer(Modifier.height(2.dp))
            Text("$likeCnt", style = MaterialTheme.typography.labelSmall, color = TextHint)
            Spacer(Modifier.height(8.dp))
            Icon(NavIcons.Comment, contentDescription = "回复", tint = TextHint, modifier = Modifier.size(16.dp).clickable { onReply() })
        }
    }
}

private fun formatSeconds(seconds: Int): String =
    if (seconds <= 0) "--:--"
    else String.format(Locale.CHINA, "%02d:%02d", seconds / 60, seconds % 60)
