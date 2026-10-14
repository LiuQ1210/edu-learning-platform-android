package com.github.learningplatform.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.data.remote.dto.CourseDetailDto
import com.github.learningplatform.data.remote.dto.CreateNoteRequest
import com.github.learningplatform.data.remote.dto.PlayInfoDto
import com.github.learningplatform.data.remote.dto.ProgressSyncRequest
import com.github.learningplatform.data.repository.CourseRepository
import com.github.learningplatform.data.repository.NoteRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext


/** 客户端类型：1-Web 2-iOS 3-Android（接口 6.4） */
private const val DEVICE_TYPE_ANDROID = 3

data class PlayerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val title: String = "",
    val playInfo: PlayInfoDto? = null,
    /** 写笔记弹窗是否可见 */
    val showNoteDialog: Boolean = false,
    /** 笔记保存中（禁用按钮防重复提交） */
    val noteSaving: Boolean = false,
    val message: String? = null
)

/**
 * 播放页。
 *
 * 播放地址不缓存：凭证带 expireTime，过期后需重新获取（见 [reload]）。
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val noteRepository: NoteRepository
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

    // ---------------------------------------------------------------- 写笔记

    fun showNoteDialog() {
        _uiState.value = _uiState.value.copy(showNoteDialog = true)
    }

    fun dismissNoteDialog() {
        // 保存中不允许关闭，否则用户以为没保存成功会重复提交
        if (_uiState.value.noteSaving) return
        _uiState.value = _uiState.value.copy(showNoteDialog = false)
    }

    /**
     * 从播放页创建视频笔记。
     *
     * `sourceType = 2`（视频/课程）、`sourceId = courseId`，
     * 并带上当前播放位置作为 `videoTimestamp` —— 这样在笔记列表里能显示
     * 「该笔记对应视频第几分钟」，是视频笔记相对文章笔记的核心差别。
     *
     * @param positionSeconds **由 UI 传入的真实播放位置**。
     *   不要在这里读 `lastKnownSeconds`：那个值只在 `syncProgress`（心跳，15-30 秒一次）
     *   和 `onPlayerDetached` 时更新，拿它当时间戳会**偏最多 30 秒**，而且不报错、很难发现。
     *   播放位置只有持有 ExoPlayer 的那一层知道，必须由 UI 传进来。
     */
    fun createNote(title: String, content: String, positionSeconds: Int) {
        if (_uiState.value.noteSaving) return
        _uiState.value = _uiState.value.copy(noteSaving = true)
        viewModelScope.launch {
            try {
                noteRepository.createNote(
                    CreateNoteRequest(
                        title = title,
                        content = content,
                        sourceType = Constants.TARGET_COURSE,
                        sourceId = courseId,
                        // 位置为 0 时不带时间戳：那是「还没开始看」，标记成 00:00 没意义
                        videoTimestamp = positionSeconds.takeIf { it > 0 }
                    )
                )
                _uiState.value = _uiState.value.copy(
                    noteSaving = false,
                    showNoteDialog = false,
                    message = "笔记已保存"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // 保存失败时**不关闭弹窗**，否则用户输入的内容会丢
                _uiState.value = _uiState.value.copy(
                    noteSaving = false,
                    message = e.readableMessage()
                )
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
