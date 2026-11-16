package com.github.learningplatform.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.data.local.TaskCenterStore
import com.github.learningplatform.data.remote.dto.CourseDetailDto
import com.github.learningplatform.data.remote.dto.CreateNoteRequest
import com.github.learningplatform.data.remote.dto.PlayInfoDto
import com.github.learningplatform.data.repository.ContentWatchRepository
import com.github.learningplatform.data.repository.CourseRepository
import com.github.learningplatform.data.repository.NoteRepository
import com.github.learningplatform.domain.model.ContentTaskType
import com.github.learningplatform.domain.model.WatchAccumulator
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
import kotlinx.coroutines.SupervisorJob

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
    private val noteRepository: NoteRepository,
    private val contentWatchRepository: ContentWatchRepository,
    private val taskCenterStore: TaskCenterStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var courseId: Long = 0L
    private var lessonId: Long? = null

    /** 实际播放的小节 ID；路由未带 lessonId 时由详情补齐，上报必须用它 */
    private var resolvedLessonId: Long? = null

    private var latestDurationSeconds: Int? = null

    /** 真实播放秒数累计；不按播放位置差值计算，避免拖动进度条刷时长 */
    private val watchAccumulator = WatchAccumulator()

    fun load(id: Long, lesson: Long?) {
        courseId = id
        lessonId = lesson
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 详情只拉一次：标题和播放地址都来自同一份响应（对齐短视频页的做法 ——
                // 播放地址就放在列表/详情数据里，不额外调接口）。原来先取详情拿标题、
                // 再调 getPlayInfo 拿地址，既多一次请求，地址又来自本地假数据。
                val detail: CourseDetailDto = courseRepository.getCourseDetail(id)
                val currentLesson = lesson?.let { targetLesson ->
                    (detail.chapters.flatMap { it.lessons } + detail.lessons)
                        .firstOrNull { it.lessonId == targetLesson }
                }
                val info = courseRepository.playInfoOf(detail, lesson)
                resolvedLessonId = info.lessonId.takeIf { it > 0L }
                latestDurationSeconds = info.duration.takeIf { it > 0 }
                _uiState.value = PlayerUiState(
                    isLoading = false,
                    title = currentLesson?.title ?: detail.title,
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
     * 播放页每秒调用一次：只有真正在播放时才累加。
     *
     * 不按播放位置差值计算——拖动进度条时位置会跳变，差值会把「没看过的部分」
     * 也算成观看时长。
     */
    fun onPlayingSecond(durationSeconds: Int) {
        if (courseId == 0L) return
        if (durationSeconds > 0) latestDurationSeconds = durationSeconds
        if (watchAccumulator.addSecond()) {
            flushWatch()
        }
    }

    /** 播放到结尾：带上 finished 标记立即上报。 */
    fun onPlaybackFinished() {
        watchAccumulator.markFinished()
        flushWatch(force = true)
    }

    /** 暂停时把已累计的秒数尽快上报，避免长时间挂起后丢失。 */
    fun onPlaybackPaused() {
        flushWatch(force = true)
    }

    /**
     * 由播放页在 onDispose 时调用：记录是否看完，并补报剩余秒数。
     */
    fun onPlayerDetached(isFinished: Boolean) {
        if (isFinished) watchAccumulator.markFinished()
        flushWatch(force = true)
    }

    /**
     * 上报累计的真实播放秒数，并把服务端结算结果写入任务中心展示缓存。
     *
     * 请求挂在 appScope 而不是 viewModelScope：退出播放页时 ViewModel 会立刻清理，
     * 挂在 viewModelScope 上的请求会被取消，最后一批秒数就丢了。
     */
    fun flushWatch(force: Boolean = false) {
        val targetLesson = resolvedLessonId ?: return
        val report = watchAccumulator.takeForReport(force) ?: return
        val duration = latestDurationSeconds
        appScope.launch {
            runCatching {
                contentWatchRepository.reportCourseVideo(
                    lessonId = targetLesson,
                    seconds = report.seconds,
                    finished = report.finished,
                    durationSeconds = duration
                )
            }.onSuccess { result ->
                // 积分是否发放由服务端判定，这里只缓存展示结果
                taskCenterStore.applyContentResult(
                    ContentTaskType.VIDEO,
                    targetLesson,
                    result
                )
            }.onFailure {
                // 网络失败：秒数放回累计器，下一次继续上报
                watchAccumulator.restore(report)
            }
        }
    }

    override fun onCleared() {
        if (courseId == 0L) return
        // 补报最后一批；appScope 上的请求不会随 ViewModel 取消
        flushWatch(force = true)
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
     *   播放位置只有持有 ExoPlayer 的那一层知道；ViewModel 里的累计器只记秒数，
     *   不保存实时位置，拿它当时间戳会偏、而且不报错、很难发现。
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
