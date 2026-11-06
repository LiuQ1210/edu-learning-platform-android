package com.github.learningplatform.ui.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.data.remote.dto.CourseDetailDto
import com.github.learningplatform.data.repository.CourseRepository
import com.github.learningplatform.data.repository.InteractionRepository
import com.github.learningplatform.data.repository.RatingRepository
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
    /** 【任务④】下拉刷新中（内容仍显示，仅顶部转圈） */
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val detail: CourseDetailDto? = null,
    val tab: Int = 0,
    /** 当前用户已提交的评分（0 = 未评分），来自 /api/v1/rating/info */
    val myScore: Double = 0.0,
    /** 评分列表（cjx 分支） */
    val ratings: List<com.github.learningplatform.data.remote.dto.RatingDto> = emptyList(),
    /** 当前用户是否已点赞该课程（interaction/status 或点击后由 toggle 返回） */
    val isLiked: Boolean = false,
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
    private val interactionRepository: InteractionRepository,
    private val ratingRepository: RatingRepository
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
                // 评分汇总与「我的评分」来自后端 rating/info；失败不影响课程详情展示
                val rating = runCatching { ratingRepository.getRatingInfo(id) }.getOrNull()
                // 评分列表（cjx 分支，当前为假数据）
                val ratings = runCatching { courseRepository.getRatings(id).list }.getOrDefault(emptyList())
                // 上报观看历史（课程）
                userRepository.reportView(Constants.TARGET_COURSE, id)
                // 进页面时同步点赞/收藏态：interaction/status?type=1 点赞、type=2 收藏
                val likedIds = runCatching {
                    interactionRepository.getActiveIds(Constants.TARGET_COURSE, 1, listOf(id))
                }.getOrDefault(emptyList())
                val favIds = runCatching {
                    interactionRepository.getActiveIds(Constants.TARGET_COURSE, 2, listOf(id))
                }.getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    detail = detail.withRating(rating?.averageScore, rating?.ratingCount),
                    ratings = ratings,
                    myScore = rating?.myScore ?: 0.0,
                    isLiked = likedIds.contains(id),
                    isFavorited = favIds.contains(id)
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

    /**
     * 【任务④：下拉强制回源】
     *
     * 详情页下拉手势触发：跳过缓存（forceRefresh = true）直接回源并覆盖写缓存。
     * 与首次加载（[load]）的区别：
     *  - 已有内容时保持显示，只开顶部下拉转圈（isRefreshing），不整页 Loading；
     *  - 失败时不清空已有内容，改为 Snackbar 提示（error 字段整页错误只用于首次加载）。
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, message = null)
            try {
                val detail = courseRepository.getCourseDetail(courseId, forceRefresh = true)
                val rating = runCatching { ratingRepository.getRatingInfo(courseId) }.getOrNull()
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    detail = detail.withRating(rating?.averageScore, rating?.ratingCount),
                    myScore = rating?.myScore ?: _uiState.value.myScore,
                    isFavorited = _uiState.value.isFavorited
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    message = e.readableMessage()
                )
            }
        }
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

    /** 点赞 / 取消点赞：后端 interaction/toggle，返回最新状态与计数 */
    fun toggleLike() {
        viewModelScope.launch {
            try {
                val result = interactionRepository.toggleLike(Constants.TARGET_COURSE, courseId)
                _uiState.value = _uiState.value.copy(
                    isLiked = result.isLiked,
                    // 计数以服务端为准，避免本地 ±1 与真实值漂移
                    detail = _uiState.value.detail?.copy(likeCount = result.likeCount),
                    message = if (result.isLiked) "已点赞" else "已取消点赞"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
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

    /** 评分范围 1-5 星；后端按 userId+courseId 覆盖更新，已有评分走 update */
    fun rate(score: Double) {
        val star = score.toInt().coerceIn(1, 5)
        viewModelScope.launch {
            try {
                val result = if (_uiState.value.myScore > 0) {
                    ratingRepository.updateRating(courseId, star)
                } else {
                    ratingRepository.addRating(courseId, star)
                }
                val detail = _uiState.value.detail
                _uiState.value = _uiState.value.copy(
                    myScore = star.toDouble(),
                    detail = detail?.copy(
                        // 后端没回字段时保留原值，别把详情页的平均分清成 0
                        averageScore = if (result.averageScore > 0) result.averageScore else detail.averageScore,
                        ratingCount = if (result.ratingCount > 0) result.ratingCount else detail.ratingCount
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

/**
 * 用评分接口的汇总值覆盖课程详情里的同名字段。
 *
 * 列表/详情接口本身也返回 avgScore / ratingCount，但用户刚评完分时
 * 评分接口的返回值更新；字段缺省（0）时保留详情原值。
 */
private fun CourseDetailDto.withRating(averageScore: Double?, ratingCount: Int?): CourseDetailDto {
    val avg = averageScore?.takeIf { it > 0 } ?: this.averageScore
    val count = ratingCount?.takeIf { it > 0 } ?: this.ratingCount
    if (avg == this.averageScore && count == this.ratingCount) return this
    return copy(averageScore = avg, ratingCount = count)
}