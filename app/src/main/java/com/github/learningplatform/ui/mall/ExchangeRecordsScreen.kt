package com.github.learningplatform.ui.mall

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import com.github.learningplatform.data.local.ExchangeRecord
import com.github.learningplatform.data.local.ExchangeStatus
import com.github.learningplatform.ui.common.AppCard
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.TagChip
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.AccentYellow
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Success
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 兑换记录：按时间倒序展示本机保存的兑换记录。
 * 后端暂无兑换接口，记录在兑换成功时写入 DataStore。
 */
@Composable
fun ExchangeRecordsScreen(
    onGoMall: () -> Unit,
    onBack: () -> Unit,
    viewModel: ExchangeRecordsViewModel = hiltViewModel()
) {
    val records by viewModel.records.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(pageGradientBrush(strong = true)),
        topBar = { BackTopBar(title = "兑换记录", onBack = onBack) }
    ) { padding ->
        if (records.isEmpty()) {
            EmptyRecords(modifier = Modifier.padding(padding), onGoMall = onGoMall)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { RecordsSummary(records) }

                items(records, key = { it.id }) { record ->
                    RecordCard(record)
                }

                item {
                    Text(
                        "记录仅保存在本机，用于前端演示",
                        fontSize = 11.sp,
                        color = TextHint,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 汇总 / 列表项
// ---------------------------------------------------------------------------

@Composable
private fun RecordsSummary(records: List<ExchangeRecord>) {
    val totalPoints = records.sumOf { it.points }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("共 ${records.size} 笔兑换", fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.weight(1f))
        Text("累计消耗 ", fontSize = 12.sp, color = TextSecondary)
        Text(
            "$totalPoints 积分",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary
        )
    }
}

@Composable
private fun RecordCard(record: ExchangeRecord) {
    val prize = demoMallPrizes.firstOrNull { it.id == record.prizeId }
    AppCard {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                prize?.visualStart ?: Primary.copy(alpha = 0.12f),
                                prize?.visualEnd ?: Primary.copy(alpha = 0.04f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(record.emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    record.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(formatRecordTime(record.createdAt), fontSize = 11.sp, color = TextHint)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "-${record.points}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(5.dp))
                TagChip(record.status.label, statusColor(record.status))
            }
        }
    }
}

private fun statusColor(status: ExchangeStatus): Color = when (status) {
    ExchangeStatus.PENDING -> Primary
    ExchangeStatus.SHIPPED -> AccentYellow
    ExchangeStatus.COMPLETED -> Success
}

private fun formatRecordTime(createdAt: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(createdAt))

// ---------------------------------------------------------------------------
// 空状态
// ---------------------------------------------------------------------------

@Composable
private fun EmptyRecords(modifier: Modifier = Modifier, onGoMall: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                NavIcons.Inbox,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("还没有兑换记录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            "用积分兑换喜欢的奖品，记录会保存在这里",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onGoMall,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            contentPadding = PaddingValues(horizontal = 32.dp),
            modifier = Modifier.height(44.dp)
        ) {
            Text("去商城看看", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
