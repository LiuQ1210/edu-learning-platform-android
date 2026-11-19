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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.graphics.Color
import com.github.learningplatform.ui.common.Avatar
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
    onOpenVideo: (Long, Long?) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenTodos: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // 导航事件：VM 完成 locate 校验/纠偏后下发，这里按归属类型调用对应跳转回调
    LaunchedEffect(viewModel) {
        viewModel.navEvent.collect { nav ->
            when (nav.targetType) {
                "1" -> onOpenArticle(nav.targetId, nav.commentId)
                "2" -> onOpenCourse(nav.targetId, null)
                "3" -> onOpenVideo(nav.targetId, nav.commentId)
            }
        }
    }

    // 评论已删除等一次性提示
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        NotificationRow(
                            item = item,
                            // 点头像：进入触发者主页
                            onClickAvatar = {
                                viewModel.markRead(item.id)
                                if (item.fromUserId != 0L) onOpenUser(item.fromUserId)
                            },
                            // 点内容区：由 VM 先 locate 校验/纠偏，再下发导航事件；关注类不跳转
                            onClickContent = {
                                viewModel.markRead(item.id)
                                viewModel.onNotificationClick(item)
                            }
                        )
                    }
                    if (uiState.hasMore) {
                        item { viewModel.loadMore() }
                    }
                }
            }
        }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun NotificationRow(
    item: NotificationUiItem,
    onClickAvatar: () -> Unit = {},
    onClickContent: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 触发者头像：点击进入其主页
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClickAvatar)
        ) {
            Avatar(url = item.fromUserAvatar, name = item.fromUserName.ifBlank { "用户" }, size = 40.dp)
            if (!item.read) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF4D6A))
                        .align(Alignment.TopEnd)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        // 内容区：点击进入具体内容
        Column(modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClickContent)
        ) {
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
