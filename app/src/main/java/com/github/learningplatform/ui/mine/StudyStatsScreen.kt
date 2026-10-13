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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Surface
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush

/**
 * 学习数据/统计页（10-10 A）。
 * 占位数据，等后端 /user/study-stats 接口联调。
 */
@Composable
fun StudyStatsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = false))
    ) {
        BackTopBar(title = "学习数据", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBigCard(label = "今日学习", value = "45", unit = "分钟", modifier = Modifier.weight(1f))
                StatBigCard(label = "连续学习", value = "7", unit = "天", modifier = Modifier.weight(1f))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("本周学习时长", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    WeekBarChart()
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("一","二","三","四","五","六","日").forEach {
                            Text(it, fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatSmallCard("已完成课程", "3", Modifier.weight(1f))
                StatSmallCard("笔记", "12", Modifier.weight(1f))
                StatSmallCard("待办", "5", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatBigCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(label, fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Primary)
                Spacer(Modifier.width(4.dp))
                Text(unit, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}

@Composable
private fun StatSmallCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun WeekBarChart() {
    val data = listOf(20, 35, 15, 50, 45, 60, 30)
    val max = data.maxOrNull() ?: 1
    Row(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        data.forEach { v ->
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height((v.toFloat() / max * 80).dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(Primary.copy(alpha = 0.85f))
            )
        }
    }
}
