package com.github.learningplatform.ui.course

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.CategoryNodeDto
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.surfaceWashBrush
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Surface
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/**
 * 全部分类（首页「查看全部」）。
 *
 * 接口 4.1 category/tree?type=2 返回多级树；这里按一级分类分卡片展示，
 * 卡片内列出二级分类，点击二级分类进入按分类筛选的课程列表。
 */
@Composable
fun CategoryAllScreen(
    onBack: () -> Unit,
    /** 选中的分类 -> 进入按该分类筛选的内容列表（带 categoryId 的搜索页） */
    onOpenCategory: (Long) -> Unit,
    viewModel: CategoryAllViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = true))
    ) {
        BackTopBar(title = "全部课程分类", onBack = onBack)

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(message = uiState.error.orEmpty(), onRetry = viewModel::load)
            uiState.tree.isEmpty() -> EmptyState(message = "暂无分类")
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.tree, key = { it.categoryId }) { node ->
                    CategoryCard(node = node, onOpenCategory = onOpenCategory)
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(node: CategoryNodeDto, onOpenCategory: (Long) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(surfaceWashBrush())
            .clickable { onOpenCategory(node.categoryId) }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Primary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = node.categoryName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(10.dp))

        if (node.children.isEmpty()) {
            Text(
                text = "暂无子分类",
                style = MaterialTheme.typography.bodySmall,
                color = Divider
            )
        } else {
            node.children.take(6).forEach { child ->
                Text(
                    text = child.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenCategory(child.categoryId) }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}