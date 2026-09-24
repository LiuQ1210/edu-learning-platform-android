package com.github.learningplatform.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.WriteNoteDialog
import com.github.learningplatform.ui.nav.NavIcons
import java.util.Locale
import kotlinx.coroutines.delay


/**
 * 播放页（接口文档 6.3 播放凭证 / 6.4 进度心跳）。
 *
 * 为什么放在全屏路由而不是主框架内：长视频需要横屏沉浸，且系统返回键语义不同
 * （先退出全屏再返回上一页）。
 *
 * 进度上报：每 20 秒一次（文档建议 15-30 秒），退出时补一次，
 * 携带 deviceType=3 表示 Android。
 */
@Composable
fun PlayerScreen(
    courseId: Long,
    lessonId: Long?,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    /*
     * 打开笔记弹窗那一刻的播放位置。
     *
     * 为什么不在弹窗里实时读：视频此时还在播，位置每帧都在变，
     * 时间戳会一直跳，用户看到「笔记位置」的数字在动会以为是 bug。
     * 打开时定格一次，语义也更对 —— 这条笔记就是记在这一刻的。
     */
    var noteTimestamp by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(courseId, lessonId) { viewModel.load(courseId, lessonId) }

    val player = remember { ExoPlayer.Builder(context).build() }

    // 播放地址就绪后装载并起播
    LaunchedEffect(uiState.playInfo?.playUrl) {
        val url = uiState.playInfo?.playUrl
        if (!url.isNullOrBlank()) {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
        }
    }

    var positionSec by remember { mutableFloatStateOf(0f) }
    var durationSec by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(true) }

    // 进度采样 + 心跳：只在播放中推进，暂停时不上报
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            // 只把位置交给 ViewModel，真正的最后一次上报在 onCleared 里发
            // （这里直接 syncProgress 会被 viewModelScope 的取消吃掉）
            val reachedEnd = player.duration > 0 &&
                player.currentPosition >= player.duration - 500
            viewModel.onPlayerDetached(
                positionSeconds = (player.currentPosition / 1000).toInt(),
                isFinished = reachedEnd
            )
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            delay(1000)
            positionSec = (player.currentPosition / 1000f).coerceAtLeast(0f)
            durationSec = (player.duration.coerceAtLeast(0L) / 1000f)
            if (player.duration > 0 && player.currentPosition >= player.duration - 500) {
                viewModel.onPlayerDetached(
                    positionSeconds = (player.currentPosition / 1000).toInt(),
                    isFinished = true
                )
                viewModel.syncProgress(
                    progressSeconds = (player.currentPosition / 1000).toInt(),
                    isFinished = 1
                )
                break
            }
        }
    }

    LaunchedEffect(player, isPlaying) {
        while (isPlaying) {
            delay(20_000)
            viewModel.syncProgress(
                progressSeconds = (player.currentPosition / 1000).toInt()
            )
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
                onRetry = { viewModel.load(courseId, lessonId) }
            )
            else -> {
                AndroidView(
                    factory = {
                        PlayerView(it).apply {
                            useController = false
                            this.player = player
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 顶部返回条
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(NavIcons.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.title.ifBlank { "课程播放" },
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        if (!uiState.playInfo?.watermarkText.isNullOrBlank()) {
                            Text(
                                text = uiState.playInfo?.watermarkText.orEmpty(),
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // 写笔记：视频笔记会带上当前播放位置
                    IconButton(onClick = {
                        // 直接取 ExoPlayer 的真实位置，不要用 ViewModel 里缓存的
                        // lastKnownSeconds —— 那个只在 15-30 秒一次的心跳里更新，会偏
                        noteTimestamp = (player.currentPosition / 1000).toInt().coerceAtLeast(0)
                        viewModel.showNoteDialog()
                    }) {
                        Icon(
                            NavIcons.Note,
                            contentDescription = "写笔记",
                            tint = Color.White
                        )
                    }
                }

                // 底部进度条
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }) {
                        Icon(
                            NavIcons.Play,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = formatTime(positionSec.toInt()),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )

                    Slider(
                        value = if (durationSec > 0f) positionSec / durationSec else 0f,
                        onValueChange = { ratio ->
                            if (durationSec > 0f) {
                                positionSec = ratio * durationSec
                                player.seekTo((positionSec * 1000).toLong())
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = formatTime(durationSec.toInt()),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }


            }
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    }

    if (uiState.showNoteDialog) {
        WriteNoteDialog(
            sourceType = Constants.TARGET_COURSE,
            sourceTitle = uiState.title,
            // 位置在弹窗打开时取一次：写笔记期间视频还在播，
            // 若每帧都重取会导致时间戳跳动，用户看到数字在变
            videoTimestamp = noteTimestamp,
            saving = uiState.noteSaving,
            onDismiss = viewModel::dismissNoteDialog,
            onSave = { title, content ->
                viewModel.createNote(title, content, noteTimestamp ?: 0)
            }
        )
    }
}

private fun formatTime(seconds: Int): String {
    if (seconds <= 0) return "00:00"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.CHINA, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.CHINA, "%02d:%02d", m, s)
}
