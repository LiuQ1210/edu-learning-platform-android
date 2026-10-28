package com.github.learningplatform.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.repository.FollowRepository
import com.github.learningplatform.data.repository.UserRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommunityProfileUiState(
    val isLoading: Boolean = false,
    /** 该用户关注了多少人 */
    val followCount: Int = 0,
    /** 该用户有多少粉丝 */
    val fansCount: Int = 0,
    /** 获赞总数：只有「我自己」能拿到（/user/home），他人后端未提供 */
    val likeReceivedCount: Int = 0,
    val isFollowed: Boolean = false,
    val submitting: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

/**
 * 社区个人主页（自己 / 他人）。
 *
 * 后端能提供的：
 *  - 自己：`/api/v1/user/home` → followCount / fansCount / likeReceivedCount
 *  - 他人：`/api/v1/follow/count?userId=` → followCount / fansCount
 *          `/api/v1/follow/status?userIds=` → 是否已关注
 *          `/api/v1/follow/add|cancel?targetUserId=` → 关注/取关
 *
 * 后端**没有**「按 userId 查他人资料/文章/获赞」的接口，
 * 所以昵称、头像、简介、文章列表仍由页面用本地样例渲染。
 */
@HiltViewModel
class CommunityProfileViewModel @Inject constructor(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityProfileUiState())
    val uiState: StateFlow<CommunityProfileUiState> = _uiState.asStateFlow()

    private var loadedUserId: Long = -1L
    private var loadedSelf: Boolean = false

    fun load(userId: Long, isSelf: Boolean) {
        loadedUserId = userId
        loadedSelf = isSelf

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                if (isSelf) {
                    val home = userRepository.getUserHome()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        followCount = home.followCount,
                        fansCount = home.fansCount,
                        likeReceivedCount = home.likeReceivedCount,
                        isFollowed = false
                    )
                } else {
                    val counts = followRepository.getFollowCount(userId)
                    val followed = followRepository.getFollowStatus(listOf(userId))
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        followCount = counts["followCount"] ?: 0,
                        fansCount = counts["fansCount"] ?: 0,
                        isFollowed = followed.contains(userId)
                    )
                }
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

    /** 关注 / 取消关注：成功后刷新粉丝数 */
    fun toggleFollow(userId: Long) {
        val state = _uiState.value
        if (state.submitting) return
        val wasFollowed = state.isFollowed
        viewModelScope.launch {
            _uiState.value = state.copy(submitting = true, error = null)
            try {
                if (wasFollowed) {
                    followRepository.cancelFollow(userId)
                } else {
                    followRepository.addFollow(userId)
                }
                // 计数以服务端为准，避免本地 ±1 与真实值漂移
                val counts = followRepository.getFollowCount(userId)
                _uiState.value = _uiState.value.copy(
                    submitting = false,
                    isFollowed = !wasFollowed,
                    fansCount = counts["fansCount"] ?: state.fansCount,
                    followCount = counts["followCount"] ?: state.followCount,
                    message = if (wasFollowed) "已取消关注" else "关注成功"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    submitting = false,
                    error = e.readableMessage()
                )
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
