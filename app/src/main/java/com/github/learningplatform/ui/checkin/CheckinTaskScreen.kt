package com.github.learningplatform.ui.checkin

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.learningplatform.ui.nav.NavIcons

/**
 * 每日签到页（平台积分体系）。
 * 每天一次签到领积分，积分可兑换免广告时长。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinTaskScreen(
    onBack: () -> Unit,
    onOpenRecords: () -> Unit,
    viewModel: CheckinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("每日签到") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(NavIcons.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Text(
                        text = "签到记录",
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable(onClick = onOpenRecords)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }
            )
        }
    ) { padding ->
        val status = uiState.status
        if (status == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶部积分卡片
            item { PointsCard(status.points, status.continuousDays, status.totalSigns) }

            // 签到按钮
            item {
                SignButton(
                    signedToday = status.signedToday,
                    todayPoints = status.todayPoints,
                    signing = uiState.signing,
                    onClick = { viewModel.sign() }
                )
            }

            // 免广告状态
            item { AdFreeStatusCard(status.adFreeUntil) }

            // 兑换列表
            item {
                Text(
                    "积分兑换免广告",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(uiState.rewards.size) { i ->
                val reward = uiState.rewards[i]
                RewardItem(
                    reward = reward,
                    canAfford = status.points >= reward.costPoints,
                    onExchange = { viewModel.exchange(reward) }
                )
            }
        }
    }
}

@Composable
private fun PointsCard(points: Int, continuousDays: Int, totalSigns: Int) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("我的积分", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
            Text(
                "$points",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("连续签到 $continuousDays 天", color = Color.White, fontSize = 13.sp)
                Text("累计签到 $totalSigns 次", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SignButton(
    signedToday: Boolean,
    todayPoints: Int,
    signing: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !signedToday && !signing,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (signedToday) Color(0xFFB0BEC5) else MaterialTheme.colorScheme.secondary
        )
    ) {
        if (signing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(
                if (signedToday) "今日已签到" else "签到 +$todayPoints 积分",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AdFreeStatusCard(adFreeUntil: Long) {
    val now = System.currentTimeMillis()
    val active = adFreeUntil > now
    val remainMinutes = if (active) ((adFreeUntil - now) / 60000).toInt() else 0
    val text = if (active) "免广告生效中，剩余 $remainMinutes 分钟" else "当前无免广告权益"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (active) Color(0xFF4CAF50) else Color.Gray)
            )
            Spacer(Modifier.width(12.dp))
            Text(text, fontSize = 14.sp)
        }
    }
}

@Composable
private fun RewardItem(
    reward: com.github.learningplatform.data.remote.dto.AdFreeRewardDto,
    canAfford: Boolean,
    onExchange: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(reward.label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${reward.costPoints} 积分", color = Color.Gray, fontSize = 13.sp)
            }
            Button(
                onClick = onExchange,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("兑换")
            }
        }
    }
}
