package com.github.learningplatform.ui.checkin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.CheckinCalendarDto
import com.github.learningplatform.data.remote.dto.CheckinRecordDto
import com.github.learningplatform.data.remote.dto.DailySignStatusDto
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush
import java.util.Calendar

/**
 * 签到记录页。
 *
 * 数据全部来自后端：
 *  - 月历：`/api/v1/checkin/calendar`（哪几天签过、服务端「今天」）
 *  - 三个统计：`/api/v1/checkin/status`（连续天数 / 累计天数 / 积分余额）
 *  - 历史列表：`/api/v1/checkin/records`
 */
@Composable
fun CheckinRecordScreen(
    onBack: () -> Unit,
    viewModel: CheckinRecordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(pageGradientBrush(strong = true)),
        topBar = { BackTopBar(title = "签到记录", onBack = onBack) }
    ) { padding ->
        when {
            state.isLoading && state.status == null ->
                Box(Modifier.fillMaxSize().padding(padding)) { LoadingState() }

            state.error != null && state.status == null ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    ErrorState(message = state.error.orEmpty(), onRetry = { viewModel.load() })
                }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { MonthCalendarCard(state.calendar) }
                item { StatsCard(state.status) }
                item {
                    Text(
                        "最近签到",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
                if (state.records.isEmpty()) {
                    item {
                        EmptyState(
                            message = "还没有签到记录",
                            modifier = Modifier.height(160.dp)
                        )
                    }
                } else {
                    items(state.records, key = { it.checkinId }) { record ->
                        HistoryRow(record)
                    }
                }
            }
        }
    }
}

/** 本月日历：高亮服务端返回的已签到日期（并标记服务端的「今天」） */
@Composable
private fun MonthCalendarCard(calendar: CheckinCalendarDto?) {
    val year = calendar?.year?.takeIf { it > 0 } ?: Calendar.getInstance().get(Calendar.YEAR)
    val month = calendar?.month?.takeIf { it > 0 } ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
    val signedDays = calendar?.days?.toSet().orEmpty()
    val todayDay = calendar?.today
        ?.split("-")
        ?.getOrNull(2)
        ?.toIntOrNull()

    val cal = remember(month, year) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    // 周日开头：Calendar.SUNDAY = 1
    val firstWeekday = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "$year 年 $month 月",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextHint,
                        modifier = Modifier.size(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            val cells = firstWeekday + daysInMonth
            val rows = (cells + 6) / 7
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (col in 0 until 7) {
                        val index = row * 7 + col
                        val day = index - firstWeekday + 1
                        if (day in 1..daysInMonth) {
                            DayCell(
                                day = day,
                                signed = day in signedDays,
                                isToday = day == todayDay
                            )
                        } else {
                            Spacer(Modifier.size(36.dp))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, signed: Boolean, isToday: Boolean) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                when {
                    signed -> Primary
                    isToday -> Primary.copy(alpha = 0.12f)
                    else -> Color.Transparent
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$day",
            style = MaterialTheme.typography.labelMedium,
            color = when {
                signed -> Color.White
                isToday -> Primary
                else -> TextSecondary
            }
        )
    }
}

@Composable
private fun StatsCard(status: DailySignStatusDto?) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("连续签到", "${status?.continuousDays ?: 0} 天")
            StatItem("累计签到", "${status?.totalSigns ?: 0} 天")
            StatItem("总积分", "${status?.points ?: 0}")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Primary)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun HistoryRow(record: CheckinRecordDto) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Primary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(record.signDate, color = TextPrimary)
                Text(
                    "连续 ${record.continuousDays} 天",
                    fontSize = 12.sp,
                    color = TextHint
                )
            }
            Text("+${record.points} 积分", color = Primary, fontWeight = FontWeight.SemiBold)
        }
    }
}
