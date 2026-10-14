package com.github.learningplatform.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

@Composable
fun PointsRulesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { BackTopBar(title = "积分获取规则", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(pageGradientBrush())
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("签到积分规则", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        RuleLine("每日签到", "每天可签到一次")
                        RuleLine("连续 1 天", "5 积分")
                        RuleLine("连续 2 天", "8 积分")
                        RuleLine("连续 3 天", "10 积分")
                        RuleLine("连续 4 天", "12 积分")
                        RuleLine("连续 5 天", "15 积分")
                        RuleLine("连续 6 天", "18 积分")
                        RuleLine("连续 7 天", "20 积分（之后循环）")
                        RuleLine("断签重置", "中断一天后连续天数重新从 1 开始")
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("积分兑换规则", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        RuleLine("10 积分", "免广告 30 分钟")
                        RuleLine("30 积分", "免广告 2 小时")
                        RuleLine("100 积分", "免广告 24 小时")
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleLine(label: String, value: String) {
    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
        Text(label, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = TextPrimary)
    }
}
