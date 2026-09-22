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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
                .padding(end = 12.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            VideoAction(
                icon = NavIcons.Favorite,
                label = formatCount(course.studentCount),
                tint = if (isLiked) Color(0xFFFF4D6A) else Color.White,
                onClick = onToggleLike
            )
            VideoAction(
                icon = NavIcons.Comment,
                label = "评论",
                onClick = onOpenComments
            )
            VideoAction(
                icon = NavIcons.Bookmark,
                label = if (isFavorited) "已收藏" else "收藏",
                tint = if (isFavorited) Color(0xFFFFC107) else Color.White,
                onClick = onToggleFavorite
            )
            VideoAction(icon = NavIcons.Share, label = "分享", onClick = onShare)
        }

        // 左下信息
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 80.dp, bottom = 120.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(url = "", name = course.instructorName, size = 32.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = course.instructorName.ifBlank { "学习平台" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(10.dp))
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

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                useController = false
                this.player = player
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = modifier
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CommentsSheet(
    loading: Boolean,
    comments: List<CommentDto>,
    error: String?,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
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
            Text(
                text = "评论",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            when {
                loading && comments.isEmpty() -> LoadingState()
                error != null && comments.isEmpty() -> ErrorState(message = error)
                comments.isEmpty() -> EmptyState(message = "还没有评论")
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageGradientBrush(strong = true)),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(comments, key = { it.commentId }) { comment ->
                        CommentRow(comment)
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
        }
    }
}

@Composable
private fun CommentRow(comment: CommentDto) {
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
                text = comment.createTime,
                style = MaterialTheme.typography.bodySmall,
                color = TextHint
            )
        }
    }
}

private fun formatSeconds(seconds: Int): String =
    if (seconds <= 0) "--:--"
    else String.format(Locale.CHINA, "%02d:%02d", seconds / 60, seconds % 60)
