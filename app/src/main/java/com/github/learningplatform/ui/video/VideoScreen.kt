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
                    VideoPage(
                        course = course,
                        isActive = page == pagerState.currentPage,
                        playUrl = uiState.playUrls[course.courseId].orEmpty(),
                        isLiked = uiState.likedIds.contains(course.courseId),
                        isFavorited = uiState.favoritedIds.contains(course.courseId),
                        likeCount = course.likeCount + uiState.likeDelta.getOrDefault(course.courseId, 0),
                        commentCount = course.commentCount,
                        favoriteCount = course.favoriteCount + uiState.favDelta.getOrDefault(course.courseId, 0),
                        shareCount = course.shareCount + uiState.shareDelta.getOrDefault(course.courseId, 0),
                        onToggleLike = { viewModel.toggleLike(course.courseId) },
                        onToggleFavorite = { viewModel.toggleFavorite(course.courseId) },
                        onOpenComments = { viewModel.openComments(course.courseId) },
                        onShare = { viewModel.share(course) },
                        onOpenDetail = { onOpenCourse(course.courseId) }
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
    isLiked: Boolean,
    isFavorited: Boolean,
    likeCount: Int,
    commentCount: Int,
    favoriteCount: Int,
    shareCount: Int,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenComments: () -> Unit,
    onShare: () -> Unit,
    onOpenDetail: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 未拿到播放凭证前先显示封面；拿到且处于当前页才真正起播
        if (isActive && playUrl.isNotBlank()) {
            ShortVideoPlayer(playUrl = playUrl, modifier = Modifier.fillMaxSize())
        } else {
            NetImage(
                url = course.coverUrl,
                modifier = Modifier.fillMaxSize(),
                radius = 0.dp
            )
            if (isActive) {
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

        // 右侧操作栏
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            VideoAction(
                icon = NavIcons.Favorite,
                label = formatCount(likeCount),
                tint = if (isLiked) Color(0xFFFF4D6A) else Color.White,
                onClick = onToggleLike
            )
            VideoAction(
                icon = NavIcons.Comment,
                label = formatCount(commentCount),
                onClick = onOpenComments
            )
            VideoAction(
                icon = NavIcons.Bookmark,
                label = formatCount(favoriteCount),
                tint = if (isFavorited) Color(0xFFFFC107) else Color.White,
                onClick = onToggleFavorite
            )
            // 分享按钮已移除
        }

        // 左下信息
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 80.dp, bottom = 80.dp)
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
    DisposableEffect(player) { onDispose { player.release() } }

    var isPlaying by remember { mutableStateOf(false) }
    var pos by remember { mutableStateOf(0L) }
    var dur by remember { mutableStateOf(0L) }

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
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
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

        // 暂停时中央显示播放按钮
        if (!isPlaying) {
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
                .padding(bottom = 90.dp, start = 12.dp, end = 12.dp)
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
    onPostComment: (String) -> Unit,
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
            var replyToUser by remember { mutableStateOf<String?>(null) }
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
                            onReply = { replyToUser = comment.userName }
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
                    TextButton(onClick = { replyToUser = null }) { Text("取消", color = TextHint) }
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
                            onPostComment(if (replyToUser != null) "回复 @$replyToUser: ${input.trim()}" else input.trim())
                            input = ""
                            replyToUser = null
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
            Text(
                text = comment.createTime + "  ·  回复 " + comment.replyCount,
                style = MaterialTheme.typography.bodySmall,
                color = TextHint
            )
            // 显示热门回复
            comment.hotReplies.forEach { reply ->
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = reply.userName + (reply.replyToName?.let { " 回复 $it" } ?: "") + "：" + reply.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
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
