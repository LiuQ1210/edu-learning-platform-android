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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.ShopAddressDto
import com.github.learningplatform.ui.common.AppCard
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.NetImage
import com.github.learningplatform.ui.common.TagChip
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Success
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush

/**
 * 兑换结果页：商品详情、默认收货信息来自后端，兑换码由确认页兑换结果带入。
 * 兑换确认页会从回退栈移除，返回直接回到商城。
 */
@Composable
fun PrizeExchangeSuccessScreen(
    productId: String,
    redemptionCode: String,
    remaining: Int,
    onViewRecords: () -> Unit,
    onBackToMall: () -> Unit,
    viewModel: PrizeExchangeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(productId) { viewModel.load(productId) }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(pageGradientBrush(strong = true)),
        topBar = { BackTopBar(title = "兑换成功", onBack = onBackToMall) },
        bottomBar = {
            SuccessBottomBar(onViewRecords = onViewRecords, onBackToMall = onBackToMall)
        }
    ) { padding ->
        val product = uiState.product
        if (product == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("商品不存在或已下架", fontSize = 14.sp, color = TextSecondary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SuccessHeader() }

            item { RedeemedPrizeCard(product) }

            item { ResultInfoCard(product = product, remaining = remaining, address = uiState.address, code = redemptionCode) }
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
private fun RedeemedPrizeCard(product: MallPrize) {
    AppCard {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MallPlaceholderEnd),
                contentAlignment = Alignment.Center
            ) {
                if (product.coverUrl.isNotBlank()) {
                    NetImage(url = product.coverUrl, modifier = Modifier.fillMaxSize(), radius = 15.dp)
                } else if (product.emoji.isNotBlank()) {
                    Text(product.emoji, fontSize = 26.sp)
                } else {
                    Icon(
                        NavIcons.School,
                        contentDescription = null,
                        tint = Primary.copy(alpha = 0.35f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    product.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (product.categoryLabel.isNotBlank()) {
                        TagChip(product.categoryLabel)
                        Spacer(Modifier.width(6.dp))
                    }
                    TagChip("已兑换", Success)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "-${product.points}",
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
private fun ResultInfoCard(product: MallPrize, remaining: Int, address: ShopAddressDto?, code: String) {
    AppCard {
        Column(Modifier.padding(16.dp)) {
            InfoRow("本次消耗", "${product.points} 积分", Primary, bold = true)
            if (remaining >= 0) {
                Spacer(Modifier.height(10.dp))
                InfoRow("剩余积分", "$remaining 积分", TextPrimary)
            }
            if (code.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                InfoRow("兑换码", code, TextPrimary)
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
            if (address != null && address.isComplete()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        address.receiverName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(maskPhone(address.receiverPhone), fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    address.detailAddress,
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

private fun ShopAddressDto.isComplete(): Boolean =
    receiverName.isNotBlank() && receiverPhone.isNotBlank() && detailAddress.isNotBlank()

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
