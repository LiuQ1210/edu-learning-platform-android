package com.github.learningplatform.ui.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.data.remote.dto.CourseDetailDto
import com.github.learningplatform.data.remote.dto.RatingDto
import com.github.learningplatform.data.repository.CourseRepository
import com.github.learningplatform.data.repository.InteractionRepository
import com.github.learningplatform.data.repository.UserRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CourseDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val detail: CourseDetailDto? = null,
    val tab: Int = 0,
    val ratings: List<RatingDto> = emptyList(),
    val myScore: Double = 0.0,
    val isFavorited: Boolean = false,
    val joining: Boolean = false,
    val message: String? = null
) {
    /**
     * 是否可直接播放。
     *
     * 判定依据（v3 文档 6.3）：整门免费、已加入课程、或该节标记为免费试看。
     * 缺了「免费试看」这一条，付费课程的试看小节会被客户端提前拦下来，
     * 用户点不动任何一节，而后端本来是允许的（3003 只针对未加入且不可试看）。
     */
    fun hasPlayPermission(detail: CourseDetailDto, lessonIsFree: Boolean = false): Boolean =
        detail.isFree == 1 || detail.isJoined || lessonIsFree
}

@HiltViewModel
class CourseDetailViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    private val interactionRepository: InteractionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourseDetailUiState())
    val uiState: StateFlow<CourseDetailUiState> = _uiState.asStateFlow()

    private var courseId: Long = 0L

    fun load(id: Long) {
        courseId = id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val detail = courseRepository.getCourseDetail(id)
                val ratings = runCatching { courseRepository.getRatings(id).list }.getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = detail,
                    ratings = ratings,
                    // 详情接口返回 isLiked/isFavorited 之外的收藏态由 8.9 维护，
                    // v3 的 6.2 未返回 isFavorited，这里先按未收藏展示。
                    isFavorited = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.readableMessage())
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(tab = index)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun notifyNeedJoin() {
        _uiState.value = _uiState.value.copy(message = "加入课程后即可观看完整内容")
    }

    fun joinOrPlay(onJoined: (() -> Unit)? = null) {
        if (_uiState.value.joining) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(joining = true)
            try {
                userRepository.joinCourse(courseId)
                _uiState.value = _uiState.value.copy(
                    joining = false,
                    detail = _uiState.value.detail?.copy(isJoined = true),
                    message = "已加入课程"
                )
                onJoined?.invoke()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // 2001 = 已加入：幂等处理，直接视为成功
                val already = (e as? com.github.learningplatform.core.network.ApiException)?.code ==
                    Constants.CODE_ALREADY_JOINED_COURSE
                _uiState.value = _uiState.value.copy(
                    joining = false,
                    detail = if (already) _uiState.value.detail?.copy(isJoined = true) else _uiState.value.detail,
                    message = if (already) "已加入课程" else e.readableMessage()
                )
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            try {
                val result = interactionRepository.toggleFavorite(Constants.TARGET_COURSE, courseId)
                _uiState.value = _uiState.value.copy(
                    isFavorited = result.isFavorited,
                    message = if (result.isFavorited) "已加入收藏" else "已取消收藏"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    /** 评分范围 0.5-5.0，步长 0.5；后端幂等覆盖 */
    fun rate(score: Double) {
        viewModelScope.launch {
            try {
                val result = courseRepository.rateCourse(courseId, score)
                val refreshed = runCatching { courseRepository.getRatings(courseId).list }
                    .getOrDefault(_uiState.value.ratings)
                _uiState.value = _uiState.value.copy(
                    myScore = score,
                    ratings = refreshed,
                    detail = _uiState.value.detail?.copy(
                        averageScore = result.averageScore,
                        ratingCount = result.ratingCount
                    ),
                    message = "评分成功"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }
}