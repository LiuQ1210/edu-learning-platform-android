package com.github.learningplatform.ui.checkin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.DailySignStatusDto
import com.github.learningplatform.domain.model.DailyTaskProgress
import com.github.learningplatform.domain.model.TaskCenterRules
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.AccentYellow
import com.github.learningplatform.ui.theme.BrandGradient
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.LocalAppDarkTheme
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Success
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush
import kotlinx.coroutines.delay

/**
 * 任务中心。
 *
 * 四类任务（口径与服务端一致）：
 *  1. 每日签到：7 天一个周期，第 1~7 天分别 +1~+7 分，断签/周期结束重新从第 1 天算；
 *  2. 观看课程视频：仅课程小节计入积分，短视频不计分；单个小节满 30 秒 +5 分，
 *     同一小节每天只计一次；
 *  3. 浏览帖子：帖子浏览满 30 秒 +5 分，同一帖子每天只计一次；
 *  4. 观看广告：完整看完一条 +10 分，同一广告每天只计一次，每天最多 3 条。
 *  课程 + 帖子两类内容当日合计不超过 50 分；广告和签到不占用该额度。
 *
 * 签到 / 积分余额走服务端；视频、帖子、广告的「今日是否已计分」由
 * [com.github.learningplatform.data.local.TaskCenterStore] 在本地记录。
 * 顶栏「规则」按钮进入完整的任务与积分规则页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinTaskScreen(
    onBack: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenMall: (balance: Int, todayPoints: Int) -> Unit,
    viewModel: CheckinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progress by viewModel.taskProgress.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAdDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    // 每次回到前台（首次进入也算一次）重新拉 /checkin/status：
    // 从课程 / 帖子页回来时余额已经变了，不刷新就会显示旧的数字。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showAdDialog) {
        AdWatchDialog(
            availableCount = progress.remainingAdCount,
            onDismiss = { showAdDialog = false },
            onClaim = {
                viewModel.watchAd(AD_CONTENT_IDS[progress.adCount.coerceAtMost(AD_CONTENT_IDS.lastIndex)])
                showAdDialog = false
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(pageGradientBrush(strong = true)),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "任务中心",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(NavIcons.ArrowBack, contentDescription = "返回", tint = TextPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = onOpenRecords) {
                        Text("签到记录", fontSize = 13.sp, color = TextSecondary)
                    }
                    RuleButton(onClick = onOpenRules)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        val status = uiState.status
        // 总积分统一以 /checkin/status 的 balance 为准；不再请求 /points/account
        val balance = status?.points

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!uiState.isLoading && uiState.loadError != null) {
                item {
                    LoadErrorBanner(
                        message = uiState.loadError.orEmpty(),
                        onRetry = viewModel::refresh
                    )
                }
            }

            item {
                TaskHeroCard(
                    balance = balance,
                    progress = progress,
                    continuousDays = status?.continuousDays,
                    totalSigns = status?.totalSigns
                )
            }

            item {
                val currentStatus = status
                if (currentStatus != null) {
                    CheckinCycleCard(
                        status = currentStatus,
                        progress = progress,
                        signing = uiState.signing,
                        onSign = viewModel::sign
                    )
                } else {
                    CheckinUnavailableCard(
                        loading = uiState.isLoading,
                        onRetry = viewModel::refresh
                    )
                }
            }

            item {
                SectionTitle(
                    title = "今日任务",
                    subtitle = "完成下列任务即可获得积分，每日 0 点重置",
                    trailing = "内容类每日 ${TaskCenterRules.CONTENT_DAILY_CAP} 分"
                )
            }

            item {
                DailyTaskRow(
                    icon = NavIcons.Video,
                    tint = Primary,
                    wash = Primary.copy(alpha = 0.12f),
                    title = "观看课程视频",
                    rule = "课程小节观看满 ${TaskCenterRules.MIN_WATCH_SECONDS} 秒 +${TaskCenterRules.VIDEO_REWARD} 分，每天 1 次；短视频不计分",
                    progressText = if (progress.videoCount > 0) {
                        "今日已看 ${progress.videoCount} 个课程小节 · 内容类共 +${progress.contentPoints} 积分"
                    } else {
                        "同一小节每天只计一次，课程 + 帖子每日合计 50 分"
                    },
                    pointsLabel = "+${TaskCenterRules.VIDEO_REWARD}",
                    actionText = "去学习",
                    actionEnabled = true,
                    done = progress.videoCount > 0,
                    onClick = {
                        TaskCenterNavBus.request(TaskCenterTarget.COURSE)
                        onBack()
                    }
                )
            }

            item {
                DailyTaskRow(
                    icon = NavIcons.Community,
                    tint = Color(0xFFE07A4F),
                    wash = Color(0xFFE07A4F).copy(alpha = 0.12f),
                    title = "浏览帖子",
                    rule = "帖子浏览满 ${TaskCenterRules.MIN_WATCH_SECONDS} 秒 +${TaskCenterRules.ARTICLE_REWARD} 分，每天 1 次",
                    progressText = if (progress.articleCount > 0) {
                        "今日已看 ${progress.articleCount} 篇 · 内容类共 +${progress.contentPoints} 积分"
                    } else {
                        "同一帖子每天只计一次，课程 + 帖子每日合计 50 分"
                    },
                    pointsLabel = "+${TaskCenterRules.ARTICLE_REWARD}",
                    actionText = "去浏览",
                    actionEnabled = true,
                    done = progress.articleCount > 0,
                    onClick = {
                        TaskCenterNavBus.request(TaskCenterTarget.COMMUNITY)
                        onBack()
                    }
                )
            }

            item {
                DailyTaskRow(
                    icon = NavIcons.Visibility,
                    tint = Color(0xFFB07A00),
                    wash = AccentYellow.copy(alpha = 0.16f),
                    title = "观看广告",
                    rule = "完整看完一条广告 +${TaskCenterRules.AD_REWARD} 分，每天最多 ${TaskCenterRules.AD_DAILY_LIMIT} 条；不占内容 50 分额度",
                    progressText = when {
                        progress.adCount > 0 ->
                            "今日已看 ${progress.adCount}/${TaskCenterRules.AD_DAILY_LIMIT} 条 · +${progress.adPoints} 积分"
                        progress.adReachedLimit -> "今日 ${TaskCenterRules.AD_DAILY_LIMIT} 条广告任务已完成"
                        else -> "同一广告每天只计 1 次"
                    },
                    pointsLabel = "+${TaskCenterRules.AD_REWARD}",
                    actionText = when {
                        progress.adReachedLimit -> "已完成"
                        progress.adCount > 0 -> "再看一条"
                        else -> "去观看"
                    },
                    actionEnabled = !progress.adReachedLimit,
                    done = progress.adReachedLimit,
                    onClick = { showAdDialog = true }
                )
            }

            item { DailyCapCard(progress = progress) }

            item { RulesEntryCard(onClick = onOpenRules) }

            item {
                SectionTitle(
                    title = "积分商城",
                    subtitle = "用积分兑换奖品，具体奖品以商城页面为准",
                    trailing = balance?.let { "余额 $it" } ?: "余额 --"
                )
            }
            item {
                PointsMallEntryCard(
                    onClick = {
                        onOpenMall(
                            balance ?: -1,
                            progress.totalPoints
                        )
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 顶部积分卡
// ---------------------------------------------------------------------------

/** 细进度条。用两段 Box 自绘，避开 Material3 进度条尾部的停止点，视觉更干净。 */
@Composable
private fun ThinProgressBar(
    fraction: Float,
    trackColor: Color,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(barColor)
        )
    }
}

@Composable
private fun TaskHeroCard(
    balance: Int?,
    progress: DailyTaskProgress,
    continuousDays: Int?,
    totalSigns: Int?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BrandGradient)
    ) {
        // 两枚半透明白圆，做出积分卡的层次，不抢文字
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 46.dp, y = (-58).dp)
                .size(170.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-52).dp, y = 48.dp)
                .size(140.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )

        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("我的积分", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "今日任务 +${progress.totalPoints}",
                    color = Color(0xFFFFE08A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = balance?.toString() ?: "--",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 52.sp
            )

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("内容积分（课程 + 帖子）", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "${progress.contentPoints} / ${TaskCenterRules.CONTENT_DAILY_CAP}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(7.dp))
            ThinProgressBar(
                fraction = progress.contentPoints.toFloat() / TaskCenterRules.CONTENT_DAILY_CAP,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp),
                trackColor = Color.White.copy(alpha = 0.20f),
                barColor = Color(0xFFFFD75E)
            )

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeroStat("连续签到", continuousDays?.let { "$it 天" } ?: "--")
                Spacer(Modifier.width(28.dp))
                HeroStat("累计签到", totalSigns?.let { "$it 次" } ?: "--")
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (progress.contentReachedCap) {
                        "内容积分已满额"
                    } else {
                        "内容积分还可获得 ${progress.remainingContentPoints} 分"
                    },
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
    }
}

// ---------------------------------------------------------------------------
// 加载失败 / 签到状态占位
// ---------------------------------------------------------------------------

/**
 * 顶部非阻塞错误条。
 *
 * 任务中心的页面不再因为某一个接口失败而整体消失：这里只提示哪一块数据没拉到，
 * 用户仍可以查看本地任务进度、积分商城，或点「重试」重新加载。
 */
@Composable
private fun LoadErrorBanner(message: String, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "!",
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 12.sp,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text("重试", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }
}

/**
 * 签到状态占位卡。
 *
 * 签到卡依赖 `/checkin/status`；接口失败时不再让整页报错，只把这一块换成通用占位，
 * 下面的任务列表、每日上限、积分商城入口仍然正常展示。
 */
@Composable
private fun CheckinUnavailableCard(loading: Boolean, onRetry: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        NavIcons.Calendar,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (loading) "正在加载签到状态…" else "签到状态暂不可用",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (loading) {
                            "任务列表和积分商城仍可正常使用"
                        } else {
                            "签到接口暂未返回数据，任务列表和积分商城仍可正常使用"
                        },
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Primary
                    )
                }
            }

            if (!loading) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("重新加载签到状态", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 签到周期卡
// ---------------------------------------------------------------------------

@Composable
private fun CheckinCycleCard(
    status: DailySignStatusDto,
    progress: DailyTaskProgress,
    signing: Boolean,
    onSign: () -> Unit
) {
    val signed = status.signedToday
    val continuousDays = status.continuousDays
    val cycleDay = if (signed) {
        TaskCenterRules.signedCycleDay(continuousDays)
    } else {
        TaskCenterRules.nextCycleDay(continuousDays)
    }
    // 签到积分单独计算，不受内容类 50 分上限影响
    val previewPoints = if (signed) {
        progress.checkinPoints
    } else {
        TaskCenterRules.nextCheckinPoints(continuousDays)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("每日签到", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "7 天一个周期，连续签到每天多 +1 分",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Text(
                    text = "第 $cycleDay / ${TaskCenterRules.CHECKIN_CYCLE_DAYS} 天",
                    color = Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Primary.copy(alpha = 0.10f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (day in 1..TaskCenterRules.CHECKIN_CYCLE_DAYS) {
                    val done = if (signed) day <= cycleDay else day < cycleDay
                    CycleDayCell(
                        day = day,
                        points = day,
                        done = done,
                        current = day == cycleDay,
                        signed = signed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "连续签到 $continuousDays 天",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = when {
                        signed && progress.checkinPoints > 0 -> "今日 +${progress.checkinPoints} 积分"
                        signed -> "今日已签到"
                        else -> "本次可得 +$previewPoints 积分"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (signed) Success else Primary
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onSign,
                enabled = !signed && !signing,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (signed) Color(0xFFB0BEC5) else MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = Color(0xFFB0BEC5).copy(alpha = 0.75f),
                    disabledContentColor = Color.White
                )
            ) {
                if (signing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = when {
                            signed -> "今日已签到"
                            else -> "签到 +$previewPoints 积分"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!signed) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "断签后本周期作废，将从下一次签到重新从第 1 天计算",
                    fontSize = 11.sp,
                    color = TextHint
                )
            }
        }
    }
}

@Composable
private fun CycleDayCell(
    day: Int,
    points: Int,
    done: Boolean,
    current: Boolean,
    signed: Boolean,
    modifier: Modifier = Modifier
) {
    val dark = LocalAppDarkTheme.current
    val background = when {
        done -> Primary
        current -> Primary.copy(alpha = if (dark) 0.24f else 0.10f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (dark) 0.55f else 0.70f)
    }
    val borderColor = when {
        current -> Primary
        done -> Color.Transparent
        else -> Color.Transparent
    }
    val primaryText = when {
        done -> Color.White
        current -> Primary
        else -> TextHint
    }
    val secondaryText = when {
        done -> Color.White.copy(alpha = 0.78f)
        current -> Primary.copy(alpha = 0.75f)
        else -> TextHint
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .then(
                if (current) Modifier.border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                else Modifier
            )
            // 已签到的当天再叠一圈白色描边，和「今天」区分开
            .then(
                if (current && done && signed) {
                    Modifier.border(1.5.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (current) "今天" else "第${day}天",
            fontSize = 9.sp,
            color = secondaryText,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "+$points",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = primaryText,
            maxLines = 1
        )
    }
}

// ---------------------------------------------------------------------------
// 每日任务卡
// ---------------------------------------------------------------------------

@Composable
private fun DailyTaskRow(
    icon: ImageVector,
    tint: Color,
    wash: Color,
    title: String,
    rule: String,
    progressText: String,
    pointsLabel: String,
    actionText: String,
    actionEnabled: Boolean,
    done: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = actionEnabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(wash),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    PointsBadge(pointsLabel)
                    if (done) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            NavIcons.Check,
                            contentDescription = "已完成",
                            tint = Success,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    rule,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = progressText,
                    fontSize = 11.sp,
                    color = if (done) Success else TextHint,
                    fontWeight = if (done) FontWeight.Medium else FontWeight.Normal
                )
            }

            Spacer(Modifier.width(10.dp))

            Button(
                onClick = onClick,
                enabled = actionEnabled,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary.copy(alpha = 0.10f),
                    contentColor = Primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = TextHint
                )
            ) {
                Text(actionText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PointsBadge(label: String) {
    val dark = LocalAppDarkTheme.current
    Text(
        text = "$label 积分",
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (dark) AccentYellow else Color(0xFF8A5A00),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AccentYellow.copy(alpha = if (dark) 0.16f else 0.18f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun DailyCapCard(progress: DailyTaskProgress) {
    val allContentDone = progress.contentReachedCap
    val allAdDone = progress.adReachedLimit
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allContentDone && allAdDone) {
                AccentYellow.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(AccentYellow.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    NavIcons.Star,
                    contentDescription = null,
                    tint = AccentYellow,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "每日积分规则",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (allContentDone) {
                        "课程 + 帖子：今日 ${progress.contentPoints}/${TaskCenterRules.CONTENT_DAILY_CAP} 分，已满额"
                    } else {
                        "课程 + 帖子：今日 ${progress.contentPoints}/${TaskCenterRules.CONTENT_DAILY_CAP} 分，还可获得 ${progress.remainingContentPoints} 分"
                    },
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "观看广告：今日 ${progress.adCount}/${TaskCenterRules.AD_DAILY_LIMIT} 条，+${progress.adPoints} 分（不占内容额度）",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "每日签到：今日 +${progress.checkinPoints} 分（单独计算）",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RulesEntryCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text("规", color = Primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "任务与积分规则",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    "签到周期、视频 / 帖子 / 广告计分与每日上限",
                    fontSize = 11.sp,
                    color = TextHint
                )
            }
            Text("查看", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(2.dp))
            Icon(
                NavIcons.ArrowForward,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = TextHint)
        }
        if (trailing != null) {
            Text(trailing, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun RuleButton(onClick: () -> Unit) {
    Text(
        text = "规则",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Primary,
        modifier = Modifier
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(50))
            .background(Primary.copy(alpha = 0.12f))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

// ---------------------------------------------------------------------------
// 广告观看（占位）
// ---------------------------------------------------------------------------

/** 占位广告内容 id：同一广告每天只计一次，每天最多 3 条 */
private val AD_CONTENT_IDS = listOf("rewarded_ad_001", "rewarded_ad_002", "rewarded_ad_003")

/** 占位广告观看时长（秒） */
private const val AD_WATCH_SECONDS = 15

@Composable
private fun AdWatchDialog(
    availableCount: Int,
    onDismiss: () -> Unit,
    onClaim: () -> Unit
) {
    var remaining by remember { mutableIntStateOf(AD_WATCH_SECONDS) }

    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
    }

    val finished = remaining <= 0
    val reward = TaskCenterRules.AD_REWARD

    AlertDialog(
        onDismissRequest = { if (finished) onDismiss() },
        title = { Text("观看广告", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF6D8BFF), Color(0xFF2F5BD6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "精品课程推荐",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "广告",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                ThinProgressBar(
                    fraction = (AD_WATCH_SECONDS - remaining).toFloat() / AD_WATCH_SECONDS,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    trackColor = Divider,
                    barColor = Primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (finished) {
                        "广告已看完，可领取 +$reward 积分"
                    } else {
                        "广告播放中，还需 $remaining 秒"
                    },
                    fontSize = 12.sp,
                    color = if (finished) Success else TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "今日还可观看 $availableCount/${TaskCenterRules.AD_DAILY_LIMIT} 条广告",
                    fontSize = 11.sp,
                    color = TextHint
                )
                if (availableCount <= 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "今日 ${TaskCenterRules.AD_DAILY_LIMIT} 条广告任务已完成，本次不再加分",
                        fontSize = 11.sp,
                        color = TextHint
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClaim, enabled = finished && availableCount > 0) {
                Text("领取 +$reward 积分")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = finished) {
                Text("关闭")
            }
        }
    )
}

// ---------------------------------------------------------------------------
// 积分商城入口
// ---------------------------------------------------------------------------

@Composable
private fun PointsMallEntryCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AccentYellow.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    NavIcons.Star,
                    contentDescription = null,
                    tint = Color(0xFFB07A00),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "去兑换奖品",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "查看可兑换奖品与所需积分",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("进入商城", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
