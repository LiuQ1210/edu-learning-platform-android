package com.github.learningplatform.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.MyCourseDto
import com.github.learningplatform.data.remote.dto.UserTargetItemDto
import com.github.learningplatform.data.remote.dto.ViewHistoryItemDto
import com.github.learningplatform.data.repository.UserRepository
import com.github.learningplatform.ui.readableMessage
import com.github.learningplatform.ui.isStaleRequest
import com.github.learningplatform.ui.PagedUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------------------------------------------------------------------
// 我的课程（3.4 / 3.6）
// ---------------------------------------------------------------------------

data class MyCoursesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val courses: List<MyCourseDto> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class MyCoursesViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyCoursesUiState())
    val uiState: StateFlow<MyCoursesUiState> = _uiState.asStateFlow()

    init { load() }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 我的课程量级有限（教学场景），一次取 100 条足够，避免引入分页组件
                val page = userRepository.getMyCourses(pageNum = 1, pageSize = 100)
                _uiState.value = MyCoursesUiState(courses = page.list)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.readableMessage())
            }
        }
    }

    fun quit(courseId: Long) {
        viewModelScope.launch {
            try {
                userRepository.quitCourse(courseId)
                _uiState.value = _uiState.value.copy(
                    courses = _uiState.value.courses.filterNot { it.courseId == courseId },
                    message = "已退出课程"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 我的收藏（3.7）
// ---------------------------------------------------------------------------

data class FavoritesUiState(

    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val targetType: Int? = null,
    val items: List<UserTargetItemDto> = emptyList(),
    val pageNum: Int = 1,
    val hasMore: Boolean = false,
    override val generation: Int = 0
) : PagedUiState

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init { load(reset = true) }

    fun refresh() = load(reset = true)

    fun selectType(targetType: Int?) {
        if (targetType == _uiState.value.targetType) return
        _uiState.value = _uiState.value.copy(targetType = targetType)
        load(reset = true)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || !state.hasMore) return
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        val snapshot = _uiState.value
        val nextPage = if (reset) 1 else snapshot.pageNum + 1
        // reset 时开启新一轮；飞行中的旧请求返回后因代次不匹配而被丢弃
        val generation = if (reset) snapshot.generation + 1 else snapshot.generation

        viewModelScope.launch {
            _uiState.value = snapshot.copy(
                isLoading = reset,
                isLoadingMore = !reset,
                error = if (reset) null else snapshot.error,
                generation = generation
            )
            try {
                val page = userRepository.getFavorites(nextPage, 20, snapshot.targetType)
                if (_uiState.value.isStaleRequest(generation)) return@launch
                val accumulated = if (reset) page.list else snapshot.items + page.list
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    items = accumulated,
                    pageNum = nextPage,
                    hasMore = accumulated.size < page.total
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (_uiState.value.isStaleRequest(generation)) return@launch
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = if (reset) e.readableMessage() else _uiState.value.error
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 观看历史（3.9 / 3.10）
// ---------------------------------------------------------------------------

data class WatchHistoryUiState(

    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val items: List<ViewHistoryItemDto> = emptyList(),
    val pageNum: Int = 1,
    val hasMore: Boolean = false,
    val message: String? = null,
    override val generation: Int = 0
) : PagedUiState

@HiltViewModel
class WatchHistoryViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchHistoryUiState())
    val uiState: StateFlow<WatchHistoryUiState> = _uiState.asStateFlow()

    init { load(reset = true) }

    fun refresh() = load(reset = true)

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || !state.hasMore) return
        load(reset = false)
    }

    /** 不传 targetType 则清空全部（3.10） */
    fun clear() {
        viewModelScope.launch {
            try {
                userRepository.clearViewHistory()
                _uiState.value = _uiState.value.copy(items = emptyList(), hasMore = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    private fun load(reset: Boolean) {
        val snapshot = _uiState.value
        val nextPage = if (reset) 1 else snapshot.pageNum + 1
        // reset 时开启新一轮；飞行中的旧请求返回后因代次不匹配而被丢弃
        val generation = if (reset) snapshot.generation + 1 else snapshot.generation

        viewModelScope.launch {
            _uiState.value = snapshot.copy(
                isLoading = reset,
                isLoadingMore = !reset,
                error = if (reset) null else snapshot.error,
                generation = generation
            )
            try {
                val page = userRepository.getViewHistory(nextPage, 20, null)
                if (_uiState.value.isStaleRequest(generation)) return@launch
                val accumulated = if (reset) page.list else snapshot.items + page.list
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = null,
                    items = accumulated,
                    pageNum = nextPage,
                    hasMore = accumulated.size < page.total
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (_uiState.value.isStaleRequest(generation)) return@launch
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = if (reset) e.readableMessage() else _uiState.value.error
                )
            }
        }
    }
}
