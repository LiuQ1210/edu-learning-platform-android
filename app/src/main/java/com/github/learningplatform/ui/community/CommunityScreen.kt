package com.github.learningplatform.ui.community

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.github.learningplatform.ui.common.Avatar
import com.github.learningplatform.ui.common.CategoryTabs
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.MetaText
import com.github.learningplatform.ui.common.NetImage
import com.github.learningplatform.ui.common.TagChip
import com.github.learningplatform.ui.common.formatCount
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.surfaceWashBrush
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Surface as SurfaceColor
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/**
 * 社区 Tab（接口文档 7.1 - 7.8）。
 *
 * 排序：1最新 2最热 3精华；分类来自 4.1 category/tree?type=1。
 * 发布入口调用 7.3 创建文章（草稿或直接提交审核）。
 */
@Composable
fun CommunityScreen(
    onOpenArticle: (Long) -> Unit,
    onOpenPublish: () -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier
        .fillMaxSize()
        .background(pageGradientBrush(strong = false))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CommunityTopBar(onOpenPublish = onOpenPublish, onOpenSearch = onOpenSearch)

            CategoryTabs(
                items = listOf(null to "全部") + uiState.categories,
                selectedId = uiState.selectedCategoryId,
                onSelect = viewModel::selectCategory
            )

            Spacer(Modifier.height(8.dp))

            SortTabs(selected = uiState.sort, onSelect = viewModel::selectSort)

            when {
                uiState.isLoading && uiState.articles.isEmpty() -> LoadingState()
                uiState.error != null && uiState.articles.isEmpty() ->
                    ErrorState(message = uiState.error.orEmpty(), onRetry = viewModel::refresh)
                uiState.articles.isEmpty() -> EmptyState(message = "还没有文章，来写第一篇吧")
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(pageGradientBrush(strong = false)),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(uiState.articles, key = { it.articleId }) { article ->
                        ArticleCard(
                            article = article,
                            onClick = { onOpenArticle(article.articleId) },
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
                            } else if (uiState.articles.isNotEmpty()) {
                                Text(
                                    text = "没有更多了",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextHint
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onOpenPublish,
            containerColor = Primary,
            contentColor = Color.White,
            // M3 的 FloatingActionButton 默认是 16dp 圆角方形；这里用正圆，
            // 和设计稿里圆润的图形语言一致
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            // 图标显式给 14dp。Edit 的路径几乎占满 24x24 视口，按默认尺寸渲染时
            // 观感比按钮本身还重；14dp 是「图标明显轻于 40dp 按钮」的比例。
            Icon(
                imageVector = NavIcons.Edit,
                contentDescription = "发布文章",
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun CommunityTopBar(onOpenPublish: () -> Unit, onOpenSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "社区",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.width(12.dp))

        // 社区搜索框：样式与首页搜索框一致（同高度、同圆角、同浅色渐变底），
        // 点击进入搜索页并直接落到「文章」类型
        Row(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(50))
                .background(surfaceWashBrush())
                .clickable(onClick = onOpenSearch)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "搜索文章",
                style = MaterialTheme.typography.bodySmall,
                color = TextHint,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                NavIcons.Search,
                contentDescription = "搜索",
                tint = Primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(4.dp))

        IconButton(onClick = onOpenPublish, modifier = Modifier.size(40.dp)) {
            Icon(NavIcons.Add, contentDescription = "发布", tint = Primary, modifier = Modifier.size(20.dp))
        }
    }
}

/** 排序切换：最新 / 最热 / 精华 */
@Composable
private fun SortTabs(selected: Int, onSelect: (Int) -> Unit) {
    val options = listOf(1 to "最新", 2 to "最热", 3 to "精华")
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(options, key = { it.first }) { (value, label) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(value) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected == value) TextPrimary else TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(if (selected == value) Primary else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun ArticleCard(
    article: ArticleDto,
    onClick: () -> Unit
) {
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
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(url = article.authorAvatar, name = article.authorName, size = 20.dp)
                Spacer(Modifier.width(6.dp))
                MetaText(article.authorName.ifBlank { "匿名" })
                Spacer(Modifier.width(10.dp))
                MetaText("${formatCount(article.viewCount)} 阅读")
                Spacer(Modifier.width(8.dp))
                MetaText("${formatCount(article.commentCount)} 评论")
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
