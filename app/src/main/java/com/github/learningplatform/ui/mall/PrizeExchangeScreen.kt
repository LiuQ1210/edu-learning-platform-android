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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.github.learningplatform.ui.theme.Error
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Success
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush
import kotlinx.coroutines.launch

/**
 * 奖品兑换确认页。
 *
 * 收货信息不再在这里填写：用户只需在「设置 → 收货信息」维护一次，
 * 本页直接展示已保存的地址卡片，点卡片可跳过去修改；没有地址时引导去添加。
 * 当前后端还没有兑换接口，确认只做地址校验，通过后进入本地的「兑换成功」页。
 */
@Composable
fun PrizeExchangeScreen(
    prize: MallPrize?,
    balance: Int,
    onEditAddress: () -> Unit,
    onSuccess: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: PrizeExchangeViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val address by viewModel.shippingAddress.collectAsStateWithLifecycle()

    if (prize == null) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.background(pageGradientBrush(strong = true)),
            topBar = { BackTopBar(title = "确认兑换", onBack = onBack) }
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

    fun submit() {
        if (!address.isComplete) {
            scope.launch { snackbarHostState.showSnackbar("请先在设置中填写收货信息") }
            return
        }
        // 后端未接入，直接进本地结果页；余额未知或扣完时传 -1，结果页隐藏剩余积分
        val remaining = (balance - prize.points).takeIf { balance >= 0 && it >= 0 } ?: -1
        scope.launch {
            viewModel.recordExchange(prize)
            onSuccess(remaining)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(pageGradientBrush(strong = true)),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { BackTopBar(title = "确认兑换", onBack = onBack) },
        bottomBar = {
            ExchangeBottomBar(prize = prize, balance = balance, onSubmit = ::submit)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { PrizeSummaryCard(prize = prize, balance = balance) }

            item { AddressCard(address = address, onEdit = onEditAddress) }

            item { ExchangeNoticeCard() }

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
// 奖品摘要
// ---------------------------------------------------------------------------

@Composable
private fun PrizeSummaryCard(prize: MallPrize, balance: Int) {
    AppCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(prize.visualStart, prize.visualEnd))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(prize.emoji, fontSize = 30.sp)
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
                        prize.originalPrice?.let { price ->
                            Spacer(Modifier.width(6.dp))
                            Text("参考价 $price", fontSize = 10.sp, color = TextHint)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${prize.points} 积分",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }

            if (balance >= 0) {
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "当前可用积分 $balance",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    val remaining = balance - prize.points
                    if (remaining >= 0) {
                        Text("兑换后剩余 $remaining", fontSize = 12.sp, color = Success)
                    } else {
                        Text("积分不足，还差 ${-remaining} 分", fontSize = 12.sp, color = Error)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 收货信息卡片（已保存的地址，点击去设置页修改）
// ---------------------------------------------------------------------------

@Composable
private fun AddressCard(address: ShippingAddress, onEdit: () -> Unit) {
    AppCard(modifier = Modifier.clickable(onClick = onEdit)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        NavIcons.Place,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("收货信息", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.weight(1f))
                Text(
                    if (address.isComplete) "修改 >" else "去添加 >",
                    fontSize = 12.sp,
                    color = Primary
                )
            }

            Spacer(Modifier.height(12.dp))
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
                Text(address.fullAddress, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
            } else {
                Text(
                    "还没有收货信息",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text("填写一次，兑换奖品时自动填入", fontSize = 11.sp, color = TextHint)
            }
        }
    }
}

internal fun maskPhone(phone: String): String =
    if (phone.length == 11) "${phone.take(3)}****${phone.takeLast(4)}" else phone

// ---------------------------------------------------------------------------
// 兑换须知 / 底部提交栏
// ---------------------------------------------------------------------------

@Composable
private fun ExchangeNoticeCard() {
    AppCard {
        Column(Modifier.padding(16.dp)) {
            Text("兑换须知", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            NoticeLine("兑换成功后积分立即扣除，不支持退回")
            NoticeLine("实物奖品预计 7 个工作日内发货")
            NoticeLine("虚拟权益将发放至当前账号")
            NoticeLine("请确认收货信息无误，信息错误可能导致无法送达")
        }
    }
}

@Composable
internal fun NoticeLine(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.55f))
        )
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
    }
}

@Composable
private fun ExchangeBottomBar(
    prize: MallPrize,
    balance: Int,
    onSubmit: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${prize.points}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("积分", fontSize = 12.sp, color = Primary)
                }
                Text(
                    if (balance >= 0) "可用 $balance 积分" else "共需 ${prize.points} 积分",
                    fontSize = 10.sp,
                    color = TextHint
                )
            }
            Button(
                onClick = onSubmit,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                contentPadding = PaddingValues(horizontal = 28.dp),
                modifier = Modifier.height(46.dp)
            ) {
                Text("确认兑换", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
