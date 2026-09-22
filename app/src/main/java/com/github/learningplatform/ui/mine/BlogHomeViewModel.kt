package com.github.learningplatform.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.data.remote.dto.ArticleDto
import com.github.learningplatform.data.remote.dto.BlogHomeDto
import com.github.learningplatform.data.repository.UserRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlogHomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val blog: BlogHomeDto? = null,
    val articles: List<ArticleDto> = emptyList()
)

/**
 * 他人博客主页（接口 3.3）。
 *
 * 该接口把作者信息与第一页文章打包返回，因此文章列表直接取 blog.articles，
 * 不再单独发一次文章列表请求。
 */
@HiltViewModel
class BlogHomeViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // 初始就置 isLoading=true：否则首帧会先渲染「用户不存在」再被数据覆盖
    private val _uiState = MutableStateFlow(BlogHomeUiState(isLoading = true))
    val uiState: StateFlow<BlogHomeUiState> = _uiState.asStateFlow()

    private var loadedUserId: Long = 0L

    fun load(userId: Long) {
        if (loadedUserId == userId && _uiState.value.blog != null) return
        loadedUserId = userId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val blog = userRepository.getBlogHome(userId)
                _uiState.value = BlogHomeUiState(
                    isLoading = false,
                    blog = blog,
                    articles = blog.articles.list
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.readableMessage())
            }
        }
    }
}