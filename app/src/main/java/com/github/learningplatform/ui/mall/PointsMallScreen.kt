package com.github.learningplatform.ui.mall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.NetImage
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.BrandGradient
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Success
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush

/**
 * 积分商城。
 *
 * 商品列表、分类来自后端 shopping 模块；积分余额与今日任务积分由任务中心通过路由参数传入。
 * 关键词、积分档位、「只看我能兑」在客户端本地过滤。
 */
@Composable
fun PointsMallScreen(
    balance: Int,
    todayPoints: Int,
    onExchangePrize: (MallPrize) -> Unit,
    onOpenRecords: () -> Unit,
    onOpenPointsLog: () -> Unit,
    onBack: () -> Unit,
    viewModel: MallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var query by rememberSaveable { mutableStateOf("") }
    var selectedPriceLabel by rememberSaveable { mutableStateOf(mallPriceRanges.first().label) }
    var affordableOnly by rememberSaveable { mutableStateOf(false) }

    val selectedRange = mallPriceRanges.firstOrNull { it.label == selectedPriceLabel }
        ?: mallPriceRanges.first()
    val balanceKnown = balance >= 0

    val filteredPrizes = remember(
        query,
        selectedPriceLabel,
        affordableOnly,
        balance,
        uiState.products
    ) {
        uiState.products.filter { prize ->
            val matchesQuery = query.isBlank() ||
                prize.title.contains(query, ignoreCase = true) ||
                prize.categoryLabel.contains(query, ignoreCase = true)
            val matchesPrice = selectedRange.contains(prize.points)
            // 余额未知（两条接口都失败）时，「只看我能兑」不参与过滤
            val matchesAffordable = !affordableOnly || !balanceKnown || prize.points <= balance
            matchesQuery && matchesPrice && matchesAffordable
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(pageGradientBrush(strong = true)),
        topBar = {
            BackTopBar(
                title = "积分商城",
                onBack = onBack,
                actions = {
                    TextButton(onClick = onOpenRecords) {
                        Text("兑换记录", fontSize = 13.sp, color = Primary)
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.error != null -> ErrorState(
                message = uiState.error.orEmpty(),
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.load() }
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    PointsSummaryCard(
                        balance = balance,
                        todayPoints = todayPoints,
                        onDetails = onOpenPointsLog
                    )
                }

                item {
                    MallSearchBar(
                        query = query,
                        onQueryChange = { query = it }
                    )
                }

                item { PrizeSectionHeader(count = filteredPrizes.size) }

                item {
                    PriceFilterRow(
                        selectedLabel = selectedPriceLabel,
                        affordableOnly = affordableOnly,
                        balanceKnown = balanceKnown,
                        onSelect = { selectedPriceLabel = it },
                        onToggleAffordable = { affordableOnly = !affordableOnly }
                    )
                }

                if (filteredPrizes.isEmpty()) {
                    item {
                        EmptyPrizeCard(
                            onReset = {
                                query = ""
                                selectedPriceLabel = mallPriceRanges.first().label
                                affordableOnly = false
                            }
                        )
                    }
                } else {
                    items(filteredPrizes.chunked(2)) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { prize ->
                                PrizeCard(
                                    prize = prize,
                                    onExchange = onExchangePrize,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 积分概览
// ---------------------------------------------------------------------------

@Composable
private fun PointsSummaryCard(
    balance: Int,
    todayPoints: Int,
    onDetails: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(BrandGradient)
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 44.dp, y = (-52).dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-36).dp, y = 40.dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("可用积分", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "积分明细 >",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onDetails)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (balance >= 0) "$balance" else "--",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 48.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "今日任务已得 +$todayPoints",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "积分兑好礼",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 搜索
// ---------------------------------------------------------------------------

@Composable
private fun MallSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    val trailing: (@Composable () -> Unit)? = if (query.isNotEmpty()) {
        {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    NavIcons.Close,
                    contentDescription = "清空搜索",
                    tint = TextHint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    } else {
        null
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(50),
        placeholder = { Text("搜索奖品、品牌或分类", color = TextHint, fontSize = 13.sp) },
        leadingIcon = {
            Icon(
                NavIcons.Search,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = trailing,
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// ---------------------------------------------------------------------------
// 奖品区
// ---------------------------------------------------------------------------

@Composable
private fun PrizeSectionHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("积分好物", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text("精选奖品，积分兑换", fontSize = 11.sp, color = TextHint)
        }
        Text("共 $count 件", fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun PriceFilterRow(
    selectedLabel: String,
    affordableOnly: Boolean,
    balanceKnown: Boolean,
    onSelect: (String) -> Unit,
    onToggleAffordable: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        if (balanceKnown) {
            item {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (affordableOnly) Primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (affordableOnly) Primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.clickable(onClick = onToggleAffordable)
                ) {
                    Text(
                        "只看我能兑",
                        fontSize = 11.sp,
                        fontWeight = if (affordableOnly) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (affordableOnly) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }
        }

        items(mallPriceRanges, key = { it.label }) { range ->
            val selected = range.label == selectedLabel
            Surface(
                shape = RoundedCornerShape(50),
                color = if (selected) Primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (selected) Primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.clickable { onSelect(range.label) }
            ) {
                Text(
                    range.label,
                    fontSize = 11.sp,
                    color = if (selected) Primary else TextSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun PrizeCard(
    prize: MallPrize,
    onExchange: (MallPrize) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.height(246.dp)
    ) {
        Column(Modifier.fillMaxHeight()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(prize.visualStart, prize.visualEnd)
                        )
                    )
            ) {
                if (prize.coverUrl.isNotBlank()) {
                    NetImage(
                        url = prize.coverUrl,
                        modifier = Modifier.fillMaxSize(),
                        radius = 0.dp
                    )
                } else if (prize.emoji.isNotBlank()) {
                    Text(
                        prize.emoji,
                        fontSize = 44.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    Icon(
                        NavIcons.School,
                        contentDescription = null,
                        tint = Primary.copy(alpha = 0.35f),
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                    )
                }
                val tag = prize.tag
                if (tag != null) {
                    Text(
                        tag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tagTint(tag),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp)
            ) {
                Text(
                    prize.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${prize.points} 积分",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    if (prize.originalPrice != null) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            prize.originalPrice,
                            fontSize = 10.sp,
                            color = TextHint,
                            textDecoration = TextDecoration.LineThrough,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                val soldOut = prize.stock <= 0
                Button(
                    onClick = { if (!soldOut) onExchange(prize) },
                    enabled = !soldOut,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    modifier = Modifier.fillMaxWidth().height(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary.copy(alpha = 0.10f),
                        contentColor = Primary,
                        disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                        disabledContentColor = TextHint
                    )
                ) {
                    Text(if (soldOut) "已兑完" else "立即兑换", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EmptyPrizeCard(onReset: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "没有找到相关奖品",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text("换个关键词或筛选条件试试", fontSize = 11.sp, color = TextHint)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onReset) {
                Text("清空筛选", color = Primary, fontSize = 12.sp)
            }
        }
    }
}

private fun tagTint(tag: String): Color = when (tag) {
    "热门", "热兑" -> Color(0xFFE85A3A)
    "新品" -> Primary
    "限量" -> Color(0xFF7A5AF8)
    "超值" -> Success
    else -> Primary
}
