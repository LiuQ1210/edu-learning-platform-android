package com.github.learningplatform.ui.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.ArticleDto
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.MetaText
import com.github.learningplatform.ui.common.NetImage
import com.github.learningplatform.ui.common.formatCount
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Success
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/** 我的发布状态：0草稿 1待审核 2已发布 3已驳回 4已下架（接口 7.6） */
private val myArticleTabs = listOf(
    null to "全部",
    2 to "已发布",
    1 to "待审核",
    0 to "草稿",
    3 to "已驳回"
)

/** 纯函数：不能在非 @Composable 上下文读取 MaterialTheme，驳回色用常量 */
private fun statusLabel(status: Int): Pair<String, Color> = when (status) {
    0 -> "草稿" to TextHint
    1 -> "待审核" to Color(0xFFF57C00)
    2 -> "已发布" to Success
    3 -> "已驳回" to Color(0xFFE53935)
    else -> "已下架" to TextHint
}

/**
 * 我的发布（接口文档 7.6 / 7.7 / 7.4 / DELETE 7.5）。
 *
 * 状态由服务端筛选（status 参数），客户端只负责分页累加。
 */
@Composable
fun MyArticlesScreen(
    onBack: () -> Unit,
    onOpenArticle: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onPublish: () -> Unit,
    viewModel: MyArticlesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<ArticleDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 删除失败等都会写 message，必须展示
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = false))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackTopBar(title = "我的发布", onBack = onBack)

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(myArticleTabs, key = { it.second }) { (status, label) ->
                    val selected = uiState.status == status
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) Primary else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { viewModel.selectStatus(status) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when {
                uiState.isLoading && uiState.articles.isEmpty() -> LoadingState()
                uiState.error != null && uiState.articles.isEmpty() ->
                    ErrorState(message = uiState.error.orEmpty(), onRetry = viewModel::refresh)
                uiState.articles.isEmpty() -> EmptyState(message = "还没有内容")
                else -> LazyColumn(
modifier = Modifier
    .fillMaxSize()
    .background(pageGradientBrush(strong = true)),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(uiState.articles, key = { it.articleId }) { article ->
                        MyArticleRow(
                            article = article,
                            onClick = { onOpenArticle(article.articleId) },
                            onEdit = { onEdit(article.articleId) },
                            onDelete = { pendingDelete = article }
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Divider)
                        )
                    }
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else if (uiState.hasMore) {
                                Text(
                                    text = "点击加载更多",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Primary,
                                    modifier = Modifier.clickable { viewModel.loadMore() }
                                )
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onPublish,
            containerColor = Primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(NavIcons.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("写文章")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    pendingDelete?.let { article ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除文章") },
            text = { Text("确定删除《${article.title}》吗？该操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(article.articleId)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun MyArticleRow(
    article: ArticleDto,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (label, color) = statusLabel(article.status)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label + " · " + article.categoryName.ifBlank { "未分类" },
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
                Spacer(Modifier.width(10.dp))
                MetaText("${formatCount(article.viewCount)} 阅读")
                Spacer(Modifier.width(8.dp))
                MetaText("${formatCount(article.likeCount)} 赞")
            }
        }

        Spacer(Modifier.width(10.dp))

        if (article.coverUrl.isNotBlank()) {
            NetImage(
                url = article.coverUrl,
                modifier = Modifier
                    .width(72.dp)
                    .height(52.dp),
                radius = 8.dp
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "编辑",
                style = MaterialTheme.typography.labelSmall,
                color = Primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            Text(
                text = "删除",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * 按标签浏览文章（接口 7.1 tagId 参数）。
 * 与社区流共用 ArticleDto，但只按标签筛选、不做分类切换。
 */
@Composable
fun TagArticlesScreen(
    tagId: Long,
    onBack: () -> Unit,
    onOpenArticle: (Long) -> Unit,
    viewModel: TagArticlesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(tagId) { viewModel.load(tagId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = false))
    ) {
        BackTopBar(title = "话题 #${uiState.tagName.ifBlank { tagId.toString() }}", onBack = onBack)

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(message = uiState.error.orEmpty(), onRetry = { viewModel.load(tagId) })
            uiState.articles.isEmpty() -> EmptyState(message = "该话题下暂无文章")
            else -> LazyColumn(
modifier = Modifier
    .fillMaxSize()
    .background(pageGradientBrush(strong = true)),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(uiState.articles, key = { it.articleId }) { article ->
                    ArticleRow(article = article, onClick = { onOpenArticle(article.articleId) })
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Divider)
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoadingMore) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else if (uiState.hasMore) {
                            Text(
                                text = "点击加载更多",
                                style = MaterialTheme.typography.bodySmall,
                                color = Primary,
                                modifier = Modifier.clickable { viewModel.loadMore() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleRow(article: ArticleDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (article.summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Row {
                MetaText(article.authorName.ifBlank { "匿名" })
                Spacer(Modifier.width(10.dp))
                MetaText("${formatCount(article.viewCount)} 阅读")
                Spacer(Modifier.width(8.dp))
                MetaText("${formatCount(article.likeCount)} 赞")
            }
        }
        if (article.coverUrl.isNotBlank()) {
            Spacer(Modifier.width(12.dp))
            NetImage(
                url = article.coverUrl,
                modifier = Modifier
                    .width(100.dp)
                    .height(72.dp),
                radius = 10.dp
            )
        }
    }
}
