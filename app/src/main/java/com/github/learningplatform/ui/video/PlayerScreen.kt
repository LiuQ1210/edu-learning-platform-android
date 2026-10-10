package com.github.learningplatform.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.AdSlotPlaceholder
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.nav.NavIcons
import kotlinx.coroutines.delay
import java.util.Locale

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
    /** 【任务⑥：中断与恢复】播放异常时置非空，覆盖层提示 + 重试；就绪后自动清空 */
    var playbackError by remember { mutableStateOf<String?>(null) }

    // 进度采样 + 心跳：只在播放中推进，暂停时不上报
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            // 【任务⑥】播放中断兜底：断网 / 源不可用 / 凭证过期时进入错误态，
            // 展示覆盖层提示并支持重试（见 retryPlayback），不再无声卡死。
            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.message?.takeIf { it.isNotBlank() } ?: "播放中断"
            }

            // 错误清除后（重试成功 / ExoPlayer 自动恢复）自动收起覆盖层
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && playbackError != null) {
                    playbackError = null
                }
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

    // 【任务⑥】中断与恢复：切后台（来电/Home 键）自动暂停，回前台自动恢复播放。
    // 记录切出前的播放态，回来时仅当之前正在播放才恢复。
    val lifecycleOwner = LocalLifecycleOwner.current
    var wasPlayingBeforeStop by remember { mutableStateOf(true) }
    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    wasPlayingBeforeStop = player.isPlaying
                    player.pause()
                }
                Lifecycle.Event.ON_START ->
                    if (wasPlayingBeforeStop && playbackError == null) player.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 【任务⑥】中断后的重试：重新装载同一地址并起播；失败会再次触发 onPlayerError
    fun retryPlayback() {
        playbackError = null
        val url = uiState.playInfo?.playUrl
        if (!url.isNullOrBlank()) {
            player.clearMediaItems()
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            player.playWhenReady = true
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

                // 【广告位预留】播放页暂停广告位：仅在暂停且无错误时显示，接入 Taku 时替换内部
                if (!isPlaying && playbackError == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AdSlotPlaceholder(
                            slotCode = "player_pause",
                            height = 96.dp
                        )
                    }
                }

                // 顶部返回条
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                // 【任务⑥】播放中断覆盖层：出错时提示原因并提供重试，避免无声卡死
                playbackError?.let { err ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.78f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "播放中断",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = err,
                                color = Color.White.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = ::retryPlayback) {
                                Text("重试")
                            }
                        }
                    }
                }


            }
        }

        // 【返回按钮·修复】抽到最外层 Box 的最高 z 序（最后绘制），
        // 绕过 AndroidView(PlayerView) 原生层对触摸的拦截；原 Row 内按钮已移除。
        // 按钮放在 TopStart + top=120dp：屏幕最顶部 y≈22-148 区域会被系统状态栏层拦截触摸
        // （实测：按钮放顶部点击无效，下移后可点击）。最高 z 序 + clickable 保证不受
        // AndroidView(PlayerView) 全屏原生层影响。
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 120.dp)
                .size(48.dp)
                .clickable {
                    android.util.Log.d("PlayerScreen", "BACK_CLICKED")
                    onBack()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(NavIcons.ArrowBack, contentDescription = "返回", tint = Color.White)
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
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
