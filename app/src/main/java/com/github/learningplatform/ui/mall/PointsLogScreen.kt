package com.github.learningplatform.ui.mall

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.PointsLogDto
import com.github.learningplatform.ui.common.AppCard
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Success
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush

/**
 * 积分明细（后端 `GET /api/v1/points/logs`）。
 *
 * 顶部按来源筛选（对应后端 type 编码），列表按时间倒序，底部「加载更多」分页。
 * 每条明细展示变动值、来源、说明、时间和变动后余额，正负分别用主色/错误色区分。
 */
@Composable
fun PointsLogScreen(
    onBack: () -> Unit,
    viewModel: PointsLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(pageGradientBrush(strong = true)),
        topBar = { BackTopBar(title = "积分明细", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FilterRow(
                selected = uiState.filterType,
                onSelect = { viewModel.setFilter(it) }
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> LoadingState()
                    uiState.error != null && uiState.logs.isEmpty() -> ErrorState(
                        message = uiState.error.orEmpty(),
                        onRetry = { viewModel.load(reset = true) }
                    )
                    uiState.logs.isEmpty() -> EmptyState(message = "暂无积分记录")
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.logs, key = { it.logId }) { log ->
                            PointsLogCard(log)
                        }
                        item {
                            ListFooter(
                                state = uiState,
                                onLoadMore = { viewModel.loadMore() }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 筛选
// ---------------------------------------------------------------------------

private data class PointsLogFilter(val label: String, val type: String?)

/** 与后端 PointsSource 的 type 编码一一对应；null = 不传，取全部 */
private val pointsLogFilters = listOf(
    PointsLogFilter("全部", null),
    PointsLogFilter("签到", "checkin"),
    PointsLogFilter("观看课程", "watchCourse"),
    PointsLogFilter("观看帖子", "watchPost"),
    PointsLogFilter("观看广告", "watchAd"),
    PointsLogFilter("商城兑换", "shopExchange")
)

@Composable
private fun FilterRow(selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pointsLogFilters) { filter ->
            val active = selected == filter.type
            Text(
                text = filter.label,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) Color.White else TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(filter.type) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 列表项 / 分页
// ---------------------------------------------------------------------------

@Composable
private fun PointsLogCard(log: PointsLogDto) {
    AppCard {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    log.sourceLabel.ifBlank { log.type.ifBlank { "积分变动" } },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (log.remark.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        log.remark,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (log.createTime.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(log.createTime, fontSize = 11.sp, color = TextHint)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                val positive = log.changeAmount >= 0
                Text(
                    text = if (positive) "+${log.changeAmount}" else "${log.changeAmount}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (positive) Success else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(4.dp))
                Text("余额 ${log.balance}", fontSize = 11.sp, color = TextHint)
            }
        }
    }
}

@Composable
private fun ListFooter(state: PointsLogUiState, onLoadMore: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isLoadingMore -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("加载中", fontSize = 12.sp, color = TextSecondary)
            }

            state.hasMore -> TextButton(onClick = onLoadMore) {
                Text("加载更多", fontSize = 13.sp, color = Primary)
            }

            state.logs.isNotEmpty() -> Text("已经到底了", fontSize = 11.sp, color = TextHint)
        }
    }
}
