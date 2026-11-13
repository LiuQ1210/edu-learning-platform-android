package com.github.learningplatform.ui.mall

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.local.ShippingAddress
import com.github.learningplatform.ui.common.AppCard
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.TagChip
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Success
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush

/**
 * 兑换结果页。
 *
 * 兑换确认页确认后进入本页，展示结果反馈、订单摘要和收货信息；
 * 兑换确认页会从回退栈移除，因此返回直接回到商城。
 * 后端暂无兑换接口，奖品与积分均为前端演示数据。
 */
@Composable
fun PrizeExchangeSuccessScreen(
    prize: MallPrize?,
    remaining: Int,
    onViewRecords: () -> Unit,
    onBackToMall: () -> Unit,
    viewModel: PrizeExchangeViewModel = hiltViewModel()
) {
    val address by viewModel.shippingAddress.collectAsStateWithLifecycle()

    if (prize == null) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.background(pageGradientBrush(strong = true)),
            topBar = { BackTopBar(title = "兑换成功", onBack = onBackToMall) }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("奖品不存在或已下架", fontSize = 14.sp, color = TextSecondary)
            }
        }
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(pageGradientBrush(strong = true)),
        topBar = { BackTopBar(title = "兑换成功", onBack = onBackToMall) },
        bottomBar = {
            SuccessBottomBar(onViewRecords = onViewRecords, onBackToMall = onBackToMall)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SuccessHeader() }

            item { RedeemedPrizeCard(prize) }

            item { ResultInfoCard(prize = prize, remaining = remaining, address = address) }

            item {
                Text(
                    "当前为示例流程，兑换功能即将上线",
                    fontSize = 11.sp,
                    color = TextHint,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 成功提示
// ---------------------------------------------------------------------------

@Composable
private fun SuccessHeader() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(Success.copy(alpha = 0.08f))
            )
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Success.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    NavIcons.Check,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("兑换成功", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("兑换已提交，可在兑换记录中查看进度", fontSize = 12.sp, color = TextSecondary)
    }
}

// ---------------------------------------------------------------------------
// 奖品与结果摘要
// ---------------------------------------------------------------------------

@Composable
private fun RedeemedPrizeCard(prize: MallPrize) {
    AppCard {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Brush.linearGradient(listOf(prize.visualStart, prize.visualEnd))),
                contentAlignment = Alignment.Center
            ) {
                Text(prize.emoji, fontSize = 26.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    prize.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TagChip(prize.categoryLabel)
                    Spacer(Modifier.width(6.dp))
                    TagChip("已兑换", Success)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "-${prize.points}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    lineHeight = 20.sp
                )
                Text("积分", fontSize = 10.sp, color = TextHint)
            }
        }
    }
}

@Composable
private fun ResultInfoCard(prize: MallPrize, remaining: Int, address: ShippingAddress) {
    AppCard {
        Column(Modifier.padding(16.dp)) {
            InfoRow("本次消耗", "${prize.points} 积分", Primary, bold = true)
            if (remaining >= 0) {
                Spacer(Modifier.height(10.dp))
                InfoRow("剩余积分", "$remaining 积分", TextPrimary)
            }

            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    NavIcons.Place,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("收货信息", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Spacer(Modifier.height(8.dp))
            if (address.isComplete) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        address.receiver,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(maskPhone(address.phone), fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    address.fullAddress,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            } else {
                Text("将使用设置中的默认收货信息", fontSize = 12.sp, color = TextHint)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color, bold: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

// ---------------------------------------------------------------------------
// 底部操作栏
// ---------------------------------------------------------------------------

@Composable
private fun SuccessBottomBar(onViewRecords: () -> Unit, onBackToMall: () -> Unit) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBackToMall,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                modifier = Modifier.weight(1f).height(46.dp)
            ) {
                Text("返回商城", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onViewRecords,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                modifier = Modifier.weight(1.3f).height(46.dp)
            ) {
                Text("查看兑换记录", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
