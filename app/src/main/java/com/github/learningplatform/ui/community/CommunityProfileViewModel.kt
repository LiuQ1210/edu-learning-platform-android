package com.github.learningplatform.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.ArticleDto
import com.github.learningplatform.data.repository.ArticleRepository
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
    /** 昵称（自己来自 /user/home；他人来自 /user/publicProfile） */
    val nickname: String = "",
    val avatar: String = "",
    val bio: String = "",
    val sexLabel: String = "",
    /** 该用户关注了多少人 */
    val followCount: Int = 0,
    /** 该用户有多少粉丝 */
    val fansCount: Int = 0,
    /** 获赞总数：自己来自 /user/home，他人来自 /user/publicProfile */
    val likeReceivedCount: Int = 0,
    val isFollowed: Boolean = false,
    /** 后端判定是否为当前登录用户本人（publicProfile.isSelf），用于校正按钮/标题语义 */
    val isSelf: Boolean = false,
    val submitting: Boolean = false,
    /** 文章：自己来自 /post/myList，他人来自 /post/list?userId= */
    val articles: List<ArticleDto> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

/**
 * 社区个人主页（自己 / 他人）。
 *
 * 后端能提供的：
 *  - 自己：`/api/v1/user/home` → 昵称/头像/简介/性别 + followCount / fansCount / likeReceivedCount
 *          `/api/v1/post/myList` → 我发布的文章
 *  - 他人：`/api/v1/user/publicProfile?userId=` → 昵称/头像/简介/性别/获赞/是否已关注
 *          `/api/v1/post/list?userId=` → 该作者已发布文章
 *          `/api/v1/follow/add|cancel?targetUserId=` → 关注/取关
 */
@HiltViewModel
class CommunityProfileViewModel @Inject constructor(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityProfileUiState())
    val uiState: StateFlow<CommunityProfileUiState> = _uiState.asStateFlow()

    private var loadedUserId: Long = -1L
    private var loadedSelf: Boolean = false

    fun load(userId: Long, isSelf: Boolean) {
        loadedUserId = userId
        loadedSelf = isSelf

        viewModelScope.launch {
            // 切换到新用户时先清空上一用户残留（昵称/头像/简介/文章/计数/关注态），
            // 避免接口返回前页面闪现旧主页数据；submitting/message 一并复位
            _uiState.value = CommunityProfileUiState(isLoading = true)
            try {
                if (isSelf) {
                    val home = userRepository.getUserHome()
                    // 我的文章：后端 /api/v1/post/myList 只返回当前登录用户的发布；
                    // 资料（昵称/头像/简介/性别）与关注/粉丝/获赞都来自 /user/home，
                    // 与底部「个人主页」读同一个接口，保证两页数据互通一致。
                    val articles = runCatching {
                        articleRepository.getMyArticles(pageNum = 1, pageSize = 20).list
                    }.getOrDefault(emptyList())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        nickname = home.userName,
                        avatar = home.avatar,
                        bio = home.bio,
                        sexLabel = home.sexLabel,
                        followCount = home.followCount,
                        fansCount = home.fansCount,
                        likeReceivedCount = home.likeReceivedCount,
                        isFollowed = false,
                        isSelf = true,
                        articles = articles
                    )
                } else {
                    // 他人：publicProfile?userId= 取公开资料（昵称/头像/简介/性别/获赞/关注状态），
                    // 文章列表复用 post/list?userId= 过滤该作者已发布文章。
                    val blog = userRepository.getBlogHome(userId)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        nickname = blog.nickname,
                        avatar = blog.avatar,
                        bio = blog.bio,
                        sexLabel = blog.sexLabel,
                        followCount = blog.followCount,
                        fansCount = blog.fansCount,
                        likeReceivedCount = blog.likeReceivedCount,
                        isFollowed = blog.isFollowing,
                        isSelf = blog.isSelf,
                        articles = blog.articles.list
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

    /** 删除我的文章（后端 /api/v1/post/remove），成功后本地移除 */
    fun deleteArticle(articleId: Long) {
        viewModelScope.launch {
            try {
                articleRepository.deleteArticle(articleId)
                _uiState.value = _uiState.value.copy(
                    articles = _uiState.value.articles.filterNot { it.articleId == articleId },
                    message = "已删除"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
