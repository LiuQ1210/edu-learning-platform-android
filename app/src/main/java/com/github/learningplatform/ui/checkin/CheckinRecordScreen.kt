package com.github.learningplatform.ui.checkin

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.pageGradientBrush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinRecordScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.background(pageGradientBrush(strong = true)),
        topBar = {
            TopAppBar(
                title = { Text("签到记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(NavIcons.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { MonthCalendarCard() }
            item { StatsCard() }
            item { HistoryTitle() }
            items(historyList.size) { i ->
                HistoryRow(historyList[i])
            }
        }
    }
}

// 本月日历：简单显示 10 月已签到的日期
@Composable
private fun MonthCalendarCard() {
    val signedDays = setOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 30, 29, 28) // 已签到日期
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("2026 年 10 月", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            // 星期头
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = TextHint, modifier = Modifier.size(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            Spacer(Modifier.height(4.dp))
            // 日期格子（10 月从周四开始，前面补 4 个空位）
            val daysInMonth = 31
            val startOffset = 4 // 10月1日是周四
            var day = 1
            var rowIdx = 0
            while (day <= daysInMonth) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (col in 0 until 7) {
                        val globalIdx = rowIdx * 7 + col
                        if (globalIdx < startOffset || day > daysInMonth) {
                            Box(Modifier.size(36.dp))
                        } else {
                            val signed = signedDays.contains(day)
                            val isToday = day == 10
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isToday -> Primary
                                                signed -> Primary.copy(alpha = 0.3f)
                                                else -> androidx.compose.ui.graphics.Color.Transparent
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$day",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when {
                                            isToday -> androidx.compose.ui.graphics.Color.White
                                            signed -> Primary
                                            else -> TextSecondary
                                        }
                                    )
                                }
                            }
                            day++
                        }
                    }
                }
                rowIdx++
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Primary.copy(alpha = 0.3f)))
                Spacer(Modifier.size(6.dp))
                Text("已签到", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.size(16.dp))
                Box(Modifier.size(10.dp).clip(CircleShape).background(Primary))
                Spacer(Modifier.size(6.dp))
                Text("今天", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun StatsCard() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatItem("连续签到", "12 天", Modifier.weight(1f))
        StatItem("累计签到", "132 天", Modifier.weight(1f))
        StatItem("总积分", "286", Modifier.weight(1f))
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Primary)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun HistoryTitle() {
    Text("最近签到", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
}

private data class HistoryItem(val date: String, val points: Int, val continuous: Boolean = true)

private val historyList = listOf(
    HistoryItem("10-09 周五", 15),
    HistoryItem("10-08 周四", 18),
    HistoryItem("10-07 周三", 20),
    HistoryItem("10-06 周二", 15),
    HistoryItem("10-05 周一", 12),
    HistoryItem("10-04 周日", 10),
    HistoryItem("10-03 周六", 8),
    HistoryItem("10-02 周五", 5),
)

@Composable
private fun HistoryRow(item: HistoryItem) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
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
            Text(item.date, modifier = Modifier.weight(1f), color = TextPrimary)
            Text("+${item.points} 积分", color = Primary, fontWeight = FontWeight.SemiBold)
        }
    }
}
