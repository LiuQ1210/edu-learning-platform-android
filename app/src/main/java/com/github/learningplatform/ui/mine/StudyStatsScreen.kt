package com.github.learningplatform.ui.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.StudyStatsDto
import com.github.learningplatform.data.remote.dto.WeekMinuteDto
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.surfaceWashBrush

/**
 * 学习数据/统计页（后端 GET /api/v1/learn/stats）。
 *
 * 今日时长、连续天数、本周七天时长、已完成课程 / 笔记 / 待办均来自服务端。
 */
@Composable
fun StudyStatsScreen(
    onBack: () -> Unit,
    viewModel: StudyStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = false))
    ) {
        BackTopBar(title = "学习数据", onBack = onBack)

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(
                message = uiState.error.orEmpty(),
                onRetry = { viewModel.load() }
            )
            uiState.stats == null -> EmptyState(message = "暂无学习数据")
            else -> StatsContent(uiState.stats!!)
        }
    }
}

@Composable
private fun StatsContent(stats: StudyStatsDto) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { OverviewRow(stats) }
        item { WeekSection(stats.weekMinutes) }
        item { CountSection(stats) }
    }
}

@Composable
private fun OverviewRow(stats: StudyStatsDto) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OverviewCard(
            title = "今日学习",
            value = "${stats.todayMinutes}",
            unit = "分钟",
            modifier = Modifier.weight(1f)
        )
        OverviewCard(
            title = "连续学习",
            value = "${stats.continuousDays}",
            unit = "天",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OverviewCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceWashBrush())
            .padding(16.dp)
    ) {
        Text(title, fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(Modifier.width(4.dp))
            Text(unit, fontSize = 12.sp, color = TextHint)
        }
    }
}

@Composable
private fun WeekSection(weekMinutes: List<WeekMinuteDto>) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceWashBrush())
            .padding(16.dp)
    ) {
        Text("本周学习时长", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        if (weekMinutes.isEmpty()) {
            Text("暂无本周数据", fontSize = 12.sp, color = TextHint)
            return@Column
        }
        val maxMinutes = weekMinutes.maxOfOrNull { it.minutes } ?: 1
        weekMinutes.forEach { day ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    day.weekday.ifBlank { day.date },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.width(44.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((day.minutes.toFloat() / maxMinutes).coerceIn(0f, 1f))
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Primary)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${day.minutes} 分钟",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    modifier = Modifier.width(64.dp)
                )
            }
        }
    }
}

@Composable
private fun CountSection(stats: StudyStatsDto) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceWashBrush())
            .padding(16.dp)
    ) {
        CountRow("已完成课程", stats.completedCourses)
        DividerLine()
        CountRow("学习笔记", stats.noteCount)
        DividerLine()
        CountRow("待办事项", stats.todoCount)
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .height(1.dp)
            .background(Divider)
    )
}

@Composable
private fun CountRow(label: String, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Text("$value", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Primary)
    }
}
