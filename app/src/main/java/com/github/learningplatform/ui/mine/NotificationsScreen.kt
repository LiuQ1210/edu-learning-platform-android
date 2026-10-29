package com.github.learningplatform.ui.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.pageGradientBrush

/**
 * 通知页面
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenCourse: (Long, Long?) -> Unit,
    onOpenArticle: (Long, Long?) -> Unit,
    onOpenVideo: (Long?) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenTodos: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush())
    ) {
        BackTopBar(title = "通知", onBack = onBack)

        // 四个 Tab：点赞、评论、关注、收藏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("点赞", "评论", "关注", "收藏")
            tabs.forEachIndexed { i, label ->
                val selected = uiState.selectedTab == i
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) Primary else MaterialTheme.colorScheme.surface)
                        .clickable { viewModel.selectTab(i) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(message = uiState.error!!, onRetry = { viewModel.refresh() })
            uiState.items.isEmpty() -> EmptyState(message = "暂无通知")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.items) { item ->
                        NotificationRow(item = item)
                    }
                    if (uiState.hasMore) {
                        item { viewModel.loadMore() }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationUiItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 类型图标背景
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when (item.category) {
                        "like" -> Primary.copy(alpha = 0.1f)
                        "comment" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        "follow" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                        "favorite" -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        else -> Primary.copy(alpha = 0.1f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (item.category) {
                    "like" -> "赞"
                    "comment" -> "评"
                    "follow" -> "关"
                    "favorite" -> "藏"
                    else -> "通"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (item.body.isNotBlank()) {
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = item.time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
