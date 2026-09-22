package com.github.learningplatform.ui.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.ArticleDto
import com.github.learningplatform.ui.common.Avatar
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.MetaText
import com.github.learningplatform.ui.common.formatCount
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.surfaceWashBrush
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Surface
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/**
 * 他人博客主页（接口文档 3.3 GET user/blog-home/{userId}，公开接口）。
 *
 * 顶部是作者信息与统计（文章数 / 总阅读 / 总点赞 / 总收藏），
 * 下方是该作者的已发布文章列表（分页由后端返回，这里做累加）。
 */
@Composable
fun BlogHomeScreen(
    userId: Long,
    onBack: () -> Unit,
    onOpenArticle: (Long) -> Unit,
    viewModel: BlogHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(userId) { viewModel.load(userId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = true))
    ) {
        BackTopBar(title = "个人主页", onBack = onBack)

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(
                message = uiState.error.orEmpty(),
                onRetry = { viewModel.load(userId) }
            )
            uiState.blog == null -> EmptyState(message = "用户不存在")
            else -> {
                val blog = uiState.blog!!
                LazyColumn(
modifier = Modifier
    .fillMaxSize()
    .background(pageGradientBrush(strong = true)),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(surfaceWashBrush())
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(url = blog.avatar, name = blog.nickname, size = 56.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = blog.nickname.ifBlank { "匿名用户" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    if (blog.bio.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        MetaText(blog.bio)
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                StatItem("文章", blog.articleCount, Modifier.weight(1f))
                                StatItem("阅读", blog.totalViewCount, Modifier.weight(1f))
                                StatItem("获赞", blog.totalLikeCount, Modifier.weight(1f))
                                StatItem("收藏", blog.totalFavoriteCount, Modifier.weight(1f))
                            }
                        }
                    }

                    if (uiState.articles.isEmpty()) {
                        item { EmptyState(message = "TA 还没有发布文章", modifier = Modifier.height(180.dp)) }
                    } else {
                        items(uiState.articles, key = { it.articleId }) { article ->
                            BlogArticleRow(article = article, onClick = { onOpenArticle(article.articleId) })
                            Box(
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Divider)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatCount(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun BlogArticleRow(article: ArticleDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
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
            MetaText(formatCount(article.viewCount) + " 阅读")
            Spacer(Modifier.width(10.dp))
            MetaText(formatCount(article.likeCount) + " 赞")
            Spacer(Modifier.width(10.dp))
            MetaText(article.publishTime)
        }
    }
}