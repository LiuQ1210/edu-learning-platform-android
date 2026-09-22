package com.github.learningplatform.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.CourseDetailDto
import com.github.learningplatform.data.remote.dto.PlayInfoDto
import com.github.learningplatform.data.remote.dto.ProgressSyncRequest
import com.github.learningplatform.data.repository.CourseRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 客户端类型：1-Web 2-iOS 3-Android（接口 6.4） */
private const val DEVICE_TYPE_ANDROID = 3

data class PlayerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val title: String = "",
    val playInfo: PlayInfoDto? = null,
    val message: String? = null
)

/**
 * 播放页。
 *
 * 播放地址不缓存：凭证带 expireTime，过期后需重新获取（见 [reload]）。
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var courseId: Long = 0L
    private var lessonId: Long? = null

    /** 最近一次上报时的播放位置，用于计算「本次有效观看时长」的增量 */
    private var lastReportedSeconds: Int = 0

    /** 最近一次已知位置；退出时的最后一次上报由 [onCleared] 发送 */
    private var lastKnownSeconds: Int = 0
    private var completed: Boolean = false

    fun load(id: Long, lesson: Long?) {
        courseId = id
        lessonId = lesson
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val detail: CourseDetailDto = courseRepository.getCourseDetail(id)
                val lessonTitle = lesson?.let { target ->
                    detail.chapters.asSequence()
                        .flatMap { it.lessons.asSequence() }
                        .firstOrNull { it.lessonId == target }
                        ?.title
                }
                val info = courseRepository.getPlayInfo(id, lesson)
                _uiState.value = PlayerUiState(
                    isLoading = false,
                    title = lessonTitle ?: detail.title,
                    playInfo = info
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.readableMessage()
                )
            }
        }
    }

    /**
     * 由播放页在 onDispose 时调用。
     *
     * ViewModel 作用域马上就要被取消，这里只记录数据，真正的请求交给 [onCleared]。
     */
    fun onPlayerDetached(positionSeconds: Int, isFinished: Boolean) {
        lastKnownSeconds = positionSeconds
        if (isFinished) completed = true
    }

    /**
     * 心跳上报。
     *
     * @param watchDuration 传 -1 表示由本方法按「距上次上报的增量」自动计算。
     *        直接传当前总位置是错的：文档 6.4 的 watchDuration 是本次有效观看时长。
     */
    fun syncProgress(progressSeconds: Int, watchDuration: Int = -1, isFinished: Int = 0) {
        if (courseId == 0L) return
        val delta = if (watchDuration >= 0) watchDuration
        else (progressSeconds - lastReportedSeconds).coerceAtLeast(0)
        lastReportedSeconds = progressSeconds
        launchSync(progressSeconds, delta, isFinished)
    }

    private fun launchSync(progressSeconds: Int, watchDuration: Int, isFinished: Int) {
        viewModelScope.launch {
            try {
                courseRepository.syncProgress(
                    ProgressSyncRequest(
                        courseId = courseId,
                        lessonId = lessonId,
                        progressSeconds = progressSeconds,
                        watchDuration = watchDuration,
                        isFinished = isFinished,
                        deviceType = DEVICE_TYPE_ANDROID
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // 忽略：下次心跳会覆盖
            }
        }
    }

    /**
     * 退出播放页的最后一次进度上报。
     *
     * 不能在 PlayerScreen 的 onDispose 里调 syncProgress：那个协程挂在 viewModelScope 上，
     * 页面被 pop 时 ViewModel 立刻清理，请求会被取消掉，最后一次进度永远丢。
     * onCleared 里改用 NonCancellable 挂在 Application 作用域上发送。
     */
    override fun onCleared() {
        super.onCleared()
        if (courseId == 0L) return
        val position = lastKnownSeconds
        val finished = if (completed) 1 else 0
        val delta = (position - lastReportedSeconds).coerceAtLeast(0)
        val request = ProgressSyncRequest(
            courseId = courseId,
            lessonId = lessonId,
            progressSeconds = position,
            watchDuration = delta,
            isFinished = finished,
            deviceType = DEVICE_TYPE_ANDROID
        )
        appScope.launch {
            withContext(NonCancellable) {
                runCatching { courseRepository.syncProgress(request) }
            }
        }
    }

    private companion object {
        /**
         * 仅用于「ViewModel 已清理但请求仍需送达」的场景。
         * 进度心跳是一次性写入，不持有任何 UI 引用，超时后自行结束。
         */
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
