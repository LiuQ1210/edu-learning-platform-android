package com.github.learningplatform.ui.mine

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
import androidx.compose.material3.LinearProgressIndicator
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
import com.github.learningplatform.data.remote.dto.DownloadRecordDto
import com.github.learningplatform.data.remote.dto.MyCourseDto
import com.github.learningplatform.data.remote.dto.UserTargetItemDto
import com.github.learningplatform.data.remote.dto.ViewHistoryItemDto
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.MetaText
import com.github.learningplatform.ui.common.NetImage
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/** 我的课程（接口文档 3.4 / 3.5 / 3.6） */
@Composable
fun MyCoursesScreen(
    onBack: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    viewModel: MyCoursesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingQuit by remember { mutableStateOf<MyCourseDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pageGradientBrush(strong = false))
        ) {
            BackTopBar(title = "我的课程", onBack = onBack)

            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null -> ErrorState(message = uiState.error.orEmpty(), onRetry = viewModel::load)
                uiState.courses.isEmpty() -> EmptyState(message = "还没有加入课程")
                else -> LazyColumn(
modifier = Modifier
    .fillMaxSize()
    .background(pageGradientBrush(strong = true)),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(uiState.courses, key = { it.courseId }) { course ->
                        MyCourseRow(
                            course = course,
                            onClick = { onOpenCourse(course.courseId) },
                            onQuit = { pendingQuit = course }
                        )
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    pendingQuit?.let { course ->
        AlertDialog(
            onDismissRequest = { pendingQuit = null },
            title = { Text("退出课程") },
            text = { Text("退出后学习进度仍会保留，确定退出《${course.title}》吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.quit(course.courseId)
                    pendingQuit = null
                }) { Text("退出", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingQuit = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun MyCourseRow(
    course: MyCourseDto,
    onClick: () -> Unit,
    onQuit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NetImage(
            url = course.coverUrl,
            modifier = Modifier
                .width(120.dp)
                .height(72.dp),
            radius = 10.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (course.studyProgress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Primary,
                trackColor = Divider
            )
            Spacer(Modifier.height(6.dp))
            Row {
                MetaText("已学 ${course.studyProgress}%")
                Spacer(Modifier.width(10.dp))
                MetaText(course.lastStudyTime)
            }
        }
        Text(
            text = "退出",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onQuit)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private val favoriteTabs = listOf(
    null to "全部",
    1 to "文章",
    2 to "视频"
)

/** 我的收藏（接口文档 3.7，多态 targetType：1文章 2视频） */
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onOpenTarget: (Int, Long) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = false))
    ) {
        BackTopBar(title = "我的收藏", onBack = onBack)

        TargetTabs(
            selected = uiState.targetType,
            onSelect = viewModel::selectType,
            tabs = favoriteTabs
        )

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(message = uiState.error.orEmpty(), onRetry = viewModel::refresh)
            uiState.items.isEmpty() -> EmptyState(message = "还没有收藏内容")
            else -> LazyColumn(
modifier = Modifier
    .fillMaxSize()
    .background(pageGradientBrush(strong = true)),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(uiState.items, key = { "${it.targetType}-${it.targetId}" }) { item ->
                    TargetRow(
                        title = item.title,
                        cover = item.cover,
                        subtitle = item.collectTime,
                        onClick = { onOpenTarget(item.targetType, item.targetId) }
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Divider)
                    )
                }
                item { LoadMoreFooter(uiState.isLoadingMore, uiState.hasMore, viewModel::loadMore) }
            }
        }
    }
}

/** 观看历史（接口文档 3.9 / 3.10 可清空） */
@Composable
fun WatchHistoryScreen(
    onBack: () -> Unit,
    onOpenTarget: (Int, Long) -> Unit,
    viewModel: WatchHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = false))
    ) {
        BackTopBar(
            title = "观看历史",
            onBack = onBack,
            actions = {
                TextButton(onClick = { showClearDialog = true }) {
                    Text("清空", color = MaterialTheme.colorScheme.error)
                }
            }
        )

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(message = uiState.error.orEmpty(), onRetry = viewModel::refresh)
            uiState.items.isEmpty() -> EmptyState(message = "暂无观看记录")
            else -> LazyColumn(
modifier = Modifier
    .fillMaxSize()
    .background(pageGradientBrush(strong = true)),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(uiState.items, key = { "${it.targetType}-${it.targetId}" }) { item ->
                    TargetRow(
                        title = item.title,
                        cover = item.cover,
                        subtitle = buildString {
                            append(item.lastViewTime)
                            if (item.progressSeconds > 0) {
                                append(" · 看到 ")
                                append(item.progressSeconds / 60)
                                append(" 分 ")
                                append(item.progressSeconds % 60)
                                append(" 秒")
                            }
                        },
                        onClick = { onOpenTarget(item.targetType, item.targetId) }
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Divider)
                    )
                }
                item { LoadMoreFooter(uiState.isLoadingMore, uiState.hasMore, viewModel::loadMore) }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空观看历史") },
            text = { Text("清空后无法恢复，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clear()
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun TargetTabs(
    selected: Int?,
    onSelect: (Int?) -> Unit,
    tabs: List<Pair<Int?, String>>
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs, key = { it.second }) { (value, label) ->
            val active = selected == value
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(50),
                color = if (active) Primary else MaterialTheme.colorScheme.surface,
                modifier = Modifier.clickable { onSelect(value) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (active) Color.White else TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TargetRow(
    title: String,
    cover: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NetImage(
            url = cover,
            modifier = Modifier
                .width(110.dp)
                .height(66.dp),
            radius = 10.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title.ifBlank { "内容已删除" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            MetaText(subtitle)
        }
    }
}

@Composable
private fun LoadMoreFooter(isLoadingMore: Boolean, hasMore: Boolean, onLoadMore: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoadingMore -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            hasMore -> Text(
                text = "点击加载更多",
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                modifier = Modifier.clickable(onClick = onLoadMore)
            )
            else -> Text(
                text = "没有更多了",
                style = MaterialTheme.typography.bodySmall,
                color = TextHint
            )
        }
    }
}
