package com.github.learningplatform.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush

/**
 * 积分规则页。
 *
 * 数据来自后端：
 *  - 签到档位 / 循环与重置说明：`/api/v1/checkin/rules`
 *  - 兑换档位：`/api/v1/points/exchange/tiers`
 */
@Composable
fun PointsRulesScreen(
    onBack: () -> Unit,
    viewModel: PointsRulesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rules = state.rules

    Scaffold(
        topBar = { BackTopBar(title = "积分获取规则", onBack = onBack) }
    ) { padding ->
        if (state.isLoading && rules == null) {
            Box(Modifier.fillMaxSize().padding(padding)) { LoadingState() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(pageGradientBrush())
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("签到积分规则", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        if (!rules?.dailyTip.isNullOrBlank()) {
                            RuleLine("每日签到", rules?.dailyTip.orEmpty())
                        }
                        rules?.tiers?.forEach { tier ->
                            RuleLine("连续 ${tier.day} 天", "${tier.points} 积分")
                        }
                        if (!rules?.resetNote.isNullOrBlank()) {
                            RuleLine("断签重置", rules?.resetNote.orEmpty())
                        }
                        if (!rules?.loopNote.isNullOrBlank()) {
                            RuleLine("循环规则", rules?.loopNote.orEmpty())
                        }
                        if (state.error != null && rules == null) {
                            Text(
                                text = state.error.orEmpty(),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("积分兑换规则", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        state.rewards.forEach { reward ->
                            RuleLine("${reward.costPoints} 积分", "免广告 ${reward.durationMinutes} 分钟")
                        }
                    }
                }
            }
            item { Box(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun RuleLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = TextPrimary)
    }
}
