package com.github.learningplatform.ui.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.data.remote.dto.ArticleDetailDto
import com.github.learningplatform.data.remote.dto.CommentDto
import com.github.learningplatform.data.remote.dto.CreateCommentRequest
import com.github.learningplatform.data.remote.dto.CreateNoteRequest
import com.github.learningplatform.data.remote.dto.ReportRequest
import com.github.learningplatform.data.repository.ArticleRepository
import com.github.learningplatform.data.repository.InteractionRepository
import com.github.learningplatform.data.repository.NoteRepository
import com.github.learningplatform.ui.readableMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


/** 分享平台：4-复制链接（接口 8.11 platform） */
private const val SHARE_PLATFORM_COPY_LINK = 4

data class ArticleDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val detail: ArticleDetailDto? = null,
    /** 当前文章是否属于本人（决定是否显示「编辑」入口） */
    val isMine: Boolean = false,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val comments: List<CommentDto> = emptyList(),
    val commentsLoading: Boolean = false,
    val commentsCursor: String? = null,
    val commentsHasMore: Boolean = false,
    val showReportDialog: Boolean = false,
    /** 写笔记弹窗是否可见 */
    val showNoteDialog: Boolean = false,
    /** 笔记保存中（禁用按钮防重复提交） */
    val noteSaving: Boolean = false,
    val message: String? = null
)

/**
 * 文章详情。
 *
 * 评论走游标分页（8.1）：首页 cursor=null，后续用上一页 nextCursor。
 * 新评论盖楼时 rootId/parentId 均传 0（接口 8.3）。
 */
@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val interactionRepository: InteractionRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleDetailUiState())
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    private var articleId: Long = 0L

    fun load(id: Long) {
        articleId = id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val detail = articleRepository.getArticleDetail(id)
                // 是否有「编辑」权限：用 7.6 我的发布列表判断（7.2 未返回作者与当前用户的关系）
                val isMine = runCatching {
                    articleRepository.getMyArticles(pageNum = 1, pageSize = 50)
                        .list.any { it.articleId == id }
                }.getOrDefault(false)

                _uiState.value = ArticleDetailUiState(
                    isLoading = false,
                    detail = detail,
                    isMine = isMine,
                    isLiked = detail.isLiked,
                    isFavorited = detail.isFavorited
                )
                loadComments(reset = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.readableMessage())
            }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun toggleLike() {
        viewModelScope.launch {
            try {
                val result = interactionRepository.toggleLike(Constants.TARGET_ARTICLE, articleId)
                _uiState.value = _uiState.value.copy(
                    isLiked = result.isLiked,
                    detail = _uiState.value.detail?.copy(likeCount = result.likeCount)
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
                val result = interactionRepository.toggleFavorite(Constants.TARGET_ARTICLE, articleId)
                _uiState.value = _uiState.value.copy(
                    isFavorited = result.isFavorited,
                    detail = _uiState.value.detail?.copy(
                        favoriteCount = if (result.isFavorited)
                            _uiState.value.detail!!.favoriteCount + 1
                        else (_uiState.value.detail!!.favoriteCount - 1).coerceAtLeast(0)
                    ),
                    message = if (result.isFavorited) "已收藏" else "已取消收藏"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    fun share() {
        viewModelScope.launch {
            try {
                interactionRepository.recordShare(
                    Constants.TARGET_ARTICLE,
                    articleId,
                    SHARE_PLATFORM_COPY_LINK
                )
                _uiState.value = _uiState.value.copy(message = "链接已复制")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    fun postComment(content: String) {
        viewModelScope.launch {
            try {
                val result = interactionRepository.createComment(
                    CreateCommentRequest(
                        targetType = Constants.TARGET_ARTICLE,
                        targetId = articleId,
                        rootId = 0,
                        parentId = 0,
                        content = content
                    )
                )
                _uiState.value = _uiState.value.copy(
                    // status=0 表示命中违禁词进入审核，评论不会立刻出现在列表
                    message = if (result.status == 1) "评论成功" else "评论已提交，等待审核",
                    detail = _uiState.value.detail?.copy(
                        commentCount = _uiState.value.detail!!.commentCount + 1
                    )
                )
                if (result.status == 1) loadComments(reset = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    fun deleteComment(commentId: Long) {
        viewModelScope.launch {
            try {
                interactionRepository.deleteComment(commentId)
                _uiState.value = _uiState.value.copy(
                    comments = _uiState.value.comments.filterNot { it.commentId == commentId },
                    message = "已删除"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    fun toggleCommentLike(commentId: Long) {
        viewModelScope.launch {
            try {
                val result = interactionRepository.toggleCommentLike(commentId)
                _uiState.value = _uiState.value.copy(
                    comments = _uiState.value.comments.map {
                        if (it.commentId == commentId)
                            it.copy(isLiked = result.isLiked, likeCount = result.likeCount)
                        else it
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
            }
        }
    }

    fun loadMoreComments() {
        if (!_uiState.value.commentsHasMore || _uiState.value.commentsLoading) return
        loadComments(reset = false)
    }

    private fun loadComments(reset: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(commentsLoading = true)
            try {
                val page = interactionRepository.getComments(
                    targetType = Constants.TARGET_ARTICLE,
                    targetId = articleId,
                    cursor = if (reset) null else _uiState.value.commentsCursor
                )
                _uiState.value = _uiState.value.copy(
                    commentsLoading = false,
                    comments = if (reset) page.list else _uiState.value.comments + page.list,
                    commentsCursor = page.nextCursor,
                    commentsHasMore = page.hasMore
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(
                    commentsLoading = false,
                    message = e.readableMessage()
                )
            }
        }
    }

    fun showReport() {
        _uiState.value = _uiState.value.copy(showReportDialog = true)
    }

    fun dismissReport() {
        _uiState.value = _uiState.value.copy(showReportDialog = false)
    }

    fun submitReport(reason: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showReportDialog = false)
            try {
                articleRepository.reportArticle(articleId, ReportRequest(reason))
                _uiState.value = _uiState.value.copy(message = "举报已提交，感谢反馈")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.value = _uiState.value.copy(message = e.readableMessage())
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
     * 从文章详情创建笔记。
     *
     * `sourceType = 1`（文章）、`sourceId = articleId`。
     * 这两个字段是接口 10.1 的必填项 —— 笔记必须归属于某篇内容，
     * 所以这个入口只能从内容页唤起，不能做成全局「新建笔记」。
     */
    fun createNote(title: String, content: String) {
        if (_uiState.value.noteSaving) return
        _uiState.value = _uiState.value.copy(noteSaving = true)
        viewModelScope.launch {
            try {
                noteRepository.createNote(
                    CreateNoteRequest(
                        title = title,
                        content = content,
                        sourceType = Constants.TARGET_ARTICLE,
                        sourceId = articleId
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
}