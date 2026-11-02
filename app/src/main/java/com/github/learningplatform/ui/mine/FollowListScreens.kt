package com.github.learningplatform.ui.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.github.learningplatform.ui.common.Avatar
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.theme.pageGradientBrush

/**
 * 关注列表 / 粉丝列表页面
 */
@Composable
fun FollowingScreen(
    onBack: () -> Unit,
    onOpenUser: (Long) -> Unit,
    viewModel: FollowingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    FollowListContent(
        title = "我的关注",
        uiState = uiState,
        onBack = onBack,
        onOpenUser = onOpenUser,
        onRefresh = { viewModel.refresh() },
        onLoadMore = { viewModel.loadMore() }
    )
}

@Composable
fun FansScreen(
    onBack: () -> Unit,
    onOpenUser: (Long) -> Unit,
    viewModel: FansViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    FollowListContent(
        title = "我的粉丝",
        uiState = uiState,
        onBack = onBack,
        onOpenUser = onOpenUser,
        onRefresh = { viewModel.refresh() },
        onLoadMore = { viewModel.loadMore() }
    )
}

@Composable
private fun FollowListContent(
    title: String,
    uiState: FollowListUiState,
    onBack: () -> Unit,
    onOpenUser: (Long) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = false))
    ) {
        BackTopBar(title = title, onBack = onBack)

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(message = uiState.error!!, onRetry = onRefresh)
            uiState.items.isEmpty() -> EmptyState(message = "暂无数据")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.items) { item ->
                        UserItemRow(
                            userName = item.userName,
                            avatar = item.avatar,
                            bio = item.bio,
                            onClick = { onOpenUser(item.userId) }
                        )
                    }
                    if (uiState.hasMore) {
                        item {
                            onLoadMore()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserItemRow(
    userName: String,
    avatar: String,
    bio: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            url = avatar,
            name = userName,
            size = 48.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = userName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (bio.isNotBlank()) {
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
