package com.github.learningplatform.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.learningplatform.domain.model.TaskCenterRules
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.AccentYellow
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Success
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush

/**
 * 任务与积分规则页。
 *
 * 规则口径与服务端一致（见 [TaskCenterRules]）：
 *  - 签到 7 天一个周期，第 1~7 天 +1~+7 分，周期结束 / 断签重新从第 1 天算；
 *  - 课程小节、帖子满 30 秒 +5 分，同一内容每天只计 1 次；短视频不计分；
 *  - 课程 + 帖子两类内容每日合计不超过 50 分；
 *  - 广告完整看完 +10 分，同一广告每天 1 次，每天最多 3 条，不占内容类 50 分额度。
 * 积分可在积分商城兑换奖品；本页只做规则说明，不展示免广告兑换档位。
 */
@Composable
fun PointsRulesScreen(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(pageGradientBrush(strong = true)),
        topBar = { BackTopBar(title = "任务与积分规则", onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { RulesIntroCard() }

            item {
                RuleSection(
                    icon = NavIcons.Calendar,
                    tint = Success,
                    title = "签到任务",
                    subtitle = "7 天一个周期，每天最多签到 1 次"
                ) {
                    CheckinCyclePreview()
                    Spacer(Modifier.height(10.dp))
                    RuleLine("每 7 天为一个周期，每人每天最多产生 1 条签到记录。", Success)
                    RuleLine("周期内第 1 天签到 +1 分，连续第 2 天 +2 分，依次递增至第 7 天 +7 分。", Success)
                    RuleLine("一个周期结束后，下一次签到重新从新周期的第 1 天计算（+1 分），连续签到天数继续累计。", Success)
                    RuleLine("周期内出现断签，本周期作废；从断签后的第一次签到起，重新从第 1 天（+1 分）开始计算。", Success)
                }
            }

            item {
                RuleSection(
                    icon = NavIcons.Video,
                    tint = Primary,
                    title = "观看课程视频",
                    subtitle = "仅课程小节计入积分，短视频不计分"
                ) {
                    RuleLine("仅课程小节（课程详情 / 播放页）计入积分，视频 Tab 的短视频不获得积分。", Primary)
                    RuleLine("单个课程小节观看满 ${TaskCenterRules.MIN_WATCH_SECONDS} 秒，才可获得 ${TaskCenterRules.VIDEO_REWARD} 积分。", Primary)
                    RuleLine("同一课程小节每天只计 1 次，次日可再次观看并重新计分。", Primary)
                    RuleLine("课程小节 + 帖子两类内容每日合计上限 ${TaskCenterRules.CONTENT_DAILY_CAP} 分。", Primary)
                    RuleLine("观看时长从视频实际开始播放后累计，拖动进度条不计入有效观看时长。", Primary)
                }
            }

            item {
                RuleSection(
                    icon = NavIcons.Community,
                    tint = Color(0xFFE07A4F),
                    title = "浏览帖子",
                    subtitle = "社区文章与帖子"
                ) {
                    RuleLine("每篇帖子浏览时长达到 ${TaskCenterRules.MIN_WATCH_SECONDS} 秒，才可获得 ${TaskCenterRules.ARTICLE_REWARD} 积分。", Color(0xFFE07A4F))
                    RuleLine("同一帖子每天只计 1 次，次日可再次浏览并重新计分。", Color(0xFFE07A4F))
                    RuleLine("课程小节 + 帖子两类内容每日合计上限 ${TaskCenterRules.CONTENT_DAILY_CAP} 分。", Color(0xFFE07A4F))
                }
            }

            item {
                RuleSection(
                    icon = NavIcons.Visibility,
                    tint = Color(0xFFB07A00),
                    title = "观看广告",
                    subtitle = "完整观看才算有效"
                ) {
                    RuleLine("完整看完一条广告，可获得 ${TaskCenterRules.AD_REWARD} 积分。", AccentYellow)
                    RuleLine("同一广告内容每天只计 1 次，次日可再次观看并重新计分。", AccentYellow)
                    RuleLine("每天最多计 ${TaskCenterRules.AD_DAILY_LIMIT} 条广告（最多 ${TaskCenterRules.AD_DAILY_LIMIT * TaskCenterRules.AD_REWARD} 分）。", AccentYellow)
                    RuleLine("广告积分不占用课程 / 帖子的 ${TaskCenterRules.CONTENT_DAILY_CAP} 分额度。", AccentYellow)
                    RuleLine("中途关闭、未播放完的广告不计分。", AccentYellow)
                }
            }

            item {
                RuleSection(
                    icon = NavIcons.Check,
                    tint = Primary,
                    title = "每日积分上限",
                    subtitle = "内容类合并计算，签到 / 广告另计"
                ) {
                    RuleLine("课程小节 + 帖子两类内容当日合计不超过 ${TaskCenterRules.CONTENT_DAILY_CAP} 分。", Primary)
                    RuleLine("达到 ${TaskCenterRules.CONTENT_DAILY_CAP} 分后，当天继续观看课程或帖子不再增加积分，次日可继续获得。", Primary)
                    RuleLine("观看广告每天最多 ${TaskCenterRules.AD_DAILY_LIMIT} 条（${TaskCenterRules.AD_DAILY_LIMIT * TaskCenterRules.AD_REWARD} 分），不占用内容类额度。", Primary)
                    RuleLine("签到积分按 7 天周期单独计算，不占用内容类额度。", Primary)
                    RuleLine("每日 0 点重置任务进度与当日积分统计；已获得的积分余额不受影响。", Primary)
                }
            }

            item {
                RuleSection(
                    icon = NavIcons.Star,
                    tint = Color(0xFF8A5A00),
                    title = "积分商城",
                    subtitle = "用积分兑换奖品"
                ) {
                    RuleLine("任务获得的积分可在积分商城兑换奖品。", Color(0xFFB07A00))
                    RuleLine("具体可兑换奖品、所需积分与库存以积分商城页面为准。", Color(0xFFB07A00))
                    RuleLine("兑换只扣减积分余额，不影响任务进度与每日积分统计。", Color(0xFFB07A00))
                }
            }

            item {
                Text(
                    "规则可能随活动调整，请以页面实际展示为准。",
                    fontSize = 11.sp,
                    color = TextHint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RulesIntroCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "做任务，赚积分",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "内容每日上限 ${TaskCenterRules.CONTENT_DAILY_CAP} 分",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "课程 + 帖子每日最多 ${TaskCenterRules.CONTENT_DAILY_CAP} 分，广告每天最多 ${TaskCenterRules.AD_DAILY_LIMIT} 条，签到单独计算。",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IntroStat("签到", "+1~7", Success)
                IntroStat("课程视频", "+${TaskCenterRules.VIDEO_REWARD}", Primary)
                IntroStat("帖子", "+${TaskCenterRules.ARTICLE_REWARD}", Color(0xFFE07A4F))
                IntroStat("广告", "+${TaskCenterRules.AD_REWARD}", AccentYellow)
            }
        }
    }
}

@Composable
private fun IntroStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun RuleSection(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(tint.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(1.dp))
                    Text(subtitle, fontSize = 11.sp, color = TextHint)
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun RuleLine(text: String, dotColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 20.sp
        )
    }
}

/** 签到 7 天周期速览：第 1~7 天对应的积分 */
@Composable
private fun CheckinCyclePreview() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (day in 1..TaskCenterRules.CHECKIN_CYCLE_DAYS) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Success.copy(alpha = 0.09f))
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("第${day}天", fontSize = 9.sp, color = TextHint, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(
                    "+$day",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Success,
                    maxLines = 1
                )
            }
        }
    }
}
