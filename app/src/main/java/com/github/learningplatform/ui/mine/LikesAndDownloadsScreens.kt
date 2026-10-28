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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.DownloadRecordDto
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.MetaText
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.SoftTealGradient
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush

/** 「我的点赞」「我的下载记录」共用的分类页签 */
private val likeTabs = listOf(
    2 to "课程",
    3 to "视频",
    1 to "文章"
)

/** 下载记录按资源类型筛，与点赞的 targetType 是两套语义，不要混用 */
private val downloadTabs = listOf(
    null to "全部",
    1 to "文档",
    2 to "资源"
)

/**
 * 我的点赞（接口文档 3.8）。
 *
 * 与「我的收藏」的区别：点赞和收藏是两个独立操作，数据也不同。
 * 页面结构复用收藏页的零件（[TargetTabs] / [TargetRow] / [LoadMoreFooter]），
 * 只换数据源与空态文案。
 */
@Composable
fun LikesScreen(
    onBack: () -> Unit,
    onOpenTarget: (Int, Long) -> Unit,
    viewModel: LikesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = false))
    ) {
        BackTopBar(title = "我的点赞", onBack = onBack)

        TargetTabs(
            selected = uiState.targetType,
            onSelect = viewModel::selectType,
            tabs = likeTabs
        )

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(
                message = uiState.error.orEmpty(),
                onRetry = viewModel::refresh
            )
            uiState.items.isEmpty() -> EmptyState(message = "还没有点赞内容")
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

/**
 * 我的下载记录（接口文档 3.11）。
 *
 * 重要：这里展示的是**服务端的下载流水**，不是本地已下载的文件。
 * 真正的离线下载（文件落盘、断点续传、存储权限）不在本阶段范围，
 * 所以点击行为是「打开对应内容页」，而不是「播放本地文件」。
 * 若后续要做离线下载，这个页面要加「本地是否已存在」的状态列。
 */
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onOpenTarget: (Int, Long) -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = false))
    ) {
        BackTopBar(title = "我的下载", onBack = onBack)

        TargetTabs(
            selected = uiState.resourceType,
            onSelect = viewModel::selectType,
            tabs = downloadTabs
        )

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(
                message = uiState.error.orEmpty(),
                onRetry = viewModel::refresh
            )
            uiState.items.isEmpty() -> EmptyState(message = "还没有下载记录")
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageGradientBrush(strong = true)),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(uiState.items, key = { it.id }) { record ->
                    DownloadRow(
                        record = record,
                        onClick = { onOpenTarget(record.resourceType, record.resourceId) }
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

/**
 * 下载记录行。
 *
 * 不用 [TargetRow]：下载记录没有封面，核心信息是「文件名 + 大小 + 时间」，
 * 强行套用带封面的行会留下一块空白。
 */
@Composable
private fun DownloadRow(
    record: DownloadRecordDto,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标块用淡青渐变，与「下载」的语义色一致
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SoftTealGradient),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = NavIcons.Inbox,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.resourceTitle.ifBlank { record.fileName },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (record.fileName.isNotBlank()) {
                    Text(
                        text = record.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Text(
                    text = formatFileSize(record.fileSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.height(2.dp))
            MetaText(record.createTime)
        }
    }
}

/**
 * 文件大小格式化。
 *
 * 用 KB/MB/GB 而不是像 [formatCount] 那样转成「万」：
 * 用户对文件大小的直觉单位是 MB，说「1.2 万字节」没有意义。
 */
internal fun formatFileSize(bytes: Long): String = when {
    bytes <= 0L -> "未知大小"
    bytes < 1024L -> "${bytes} B"
    bytes < 1024L * 1024L -> String.format(java.util.Locale.ROOT, "%.0f KB", bytes / 1024.0)
    bytes < 1024L * 1024L * 1024L -> String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024.0)
    else -> String.format(java.util.Locale.ROOT, "%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0)
}
