package com.github.learningplatform.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.LearningCourseDto
import com.github.learningplatform.data.remote.dto.StudyStatsDto
import com.github.learningplatform.data.repository.StudyRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 课程列表筛选值，与后端 `/learn/courses` 的 status 一一对应 */
object CourseStatus {
    const val LEARNING = "learning"
    const val COMPLETED = "completed"
}

data class StudyStatsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val stats: StudyStatsDto? = null,
    /** 当前课程列表筛选：learning / completed */
    val courseStatus: String = CourseStatus.LEARNING,
    /** 课程列表；null = 还没加载 */
    val courses: List<LearningCourseDto>? = null,
    val coursesLoading: Boolean = false,
    val coursesError: String? = null
)

/**
 * 学习数据页：总览（GET /api/v1/learn/stats）+ 我的课程（GET /api/v1/learn/courses）。
 *
 * 总览失败整页报错；课程列表失败只影响课程区块，不影响其他统计。
 * 切筛选会取消上一次课程请求，避免旧结果覆盖新结果。
 */
@HiltViewModel
class StudyStatsViewModel @Inject constructor(
    private val studyRepository: StudyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyStatsUiState())
    val uiState: StateFlow<StudyStatsUiState> = _uiState.asStateFlow()

    private var courseJob: Job? = null

    /** 页面停留期间的轮询任务；页面不可见就取消，避免后台空跑 */
    private var pollJob: Job? = null

    init { load() }

    /** 页面可见时每 [POLL_INTERVAL_MS] 静默刷新一次，做到停留页面时数据自动更新 */
    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                refresh()
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /** 首次进入整页 loading；[refresh] 静默刷新，不闪加载态 */
    fun load(showLoading: Boolean = true) {
        if (showLoading) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        }
        viewModelScope.launch {
            try {
                val stats = studyRepository.getStats()
                _uiState.value = _uiState.value.copy(isLoading = false, error = null, stats = stats)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (showLoading) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.readableMessage())
                }
                return@launch
            }
            loadCourses(_uiState.value.courseStatus, silent = !showLoading)
        }
    }

    /** 回到学习数据页（从播放器 / 详情页返回、切回前台）时静默刷新统计与课程进度 */
    fun refresh() = load(showLoading = false)

    fun setCourseStatus(status: String) {
        if (_uiState.value.courseStatus == status) return
        _uiState.value = _uiState.value.copy(
            courseStatus = status,
            courses = null,
            coursesError = null
        )
        loadCourses(status)
    }

    fun loadCourses(status: String, silent: Boolean = false) {
        courseJob?.cancel()
        courseJob = viewModelScope.launch {
            if (!silent) {
                _uiState.value = _uiState.value.copy(coursesLoading = true, coursesError = null)
            }
            try {
                val page = studyRepository.getLearningCourses(status)
                _uiState.value = _uiState.value.copy(coursesLoading = false, courses = page.list)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    coursesLoading = false,
                    coursesError = e.readableMessage()
                )
            }
        }
    }

    companion object {
        /**
         * 轮询间隔：15 秒。
         *
         * 课程播放与文章阅读都是每 20 秒上报一次增量（WatchAccumulator），
         * 间隔取 15 秒小于上报节奏，保证「阅读/播放结束后最多十几秒」页面数字就更新，
         * 又不会像秒级轮询那样频繁打接口。
         */
        private const val POLL_INTERVAL_MS = 15_000L
    }
}
