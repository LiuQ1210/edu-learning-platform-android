package com.github.learningplatform.ui.mine

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.ui.common.Avatar
import com.github.learningplatform.ui.common.MetaText
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.WarmYellowGradient
import com.github.learningplatform.ui.theme.surfaceWashBrush
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Surface
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/**
 * 个人主页（对齐 UI 稿第四个 Tab）。
 *
 * 数据来源：
 *  3.1 user/profile            —— 头像 / 昵称 / 连续签到天数
 *  3.4 user/courses 等由各子页面自行拉取，这里只做入口聚合。
 */
@Composable
fun ProfileScreen(
    onOpenCheckin: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenMyCourses: () -> Unit,
    onOpenMyArticles: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenLikes: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenTodos: () -> Unit,
    onOpenSettings: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = true))
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "个人主页",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Box {
                Icon(
                    NavIcons.Notifications,
                    contentDescription = "通知",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
                if (uiState.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            ProfileHeader(
                nickname = uiState.nickname,
                avatar = uiState.avatar,
                bio = uiState.bio,
                continueSignDay = uiState.continueSignDay,
                signedToday = uiState.signedToday,
                onOpenCheckin = onOpenCheckin,
                onOpenEditProfile = onOpenEditProfile
            )

            // 资料拉取失败时明确告知，否则页面只显示空昵称，用户不知道是没登录还是网络问题
            if (uiState.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = uiState.error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceWashBrush())
            ) {
                MenuRow(NavIcons.Video, "我的课程", onOpenMyCourses)
                MenuDivider()
                MenuRow(NavIcons.Community, "我的文章", onOpenMyArticles)
                MenuDivider()
                MenuRow(NavIcons.History, "观看历史", onOpenHistory)
                MenuDivider()
                MenuRow(NavIcons.Star, "我的收藏", onOpenFavorites)
                MenuDivider()
                MenuRow(NavIcons.Favorite, "我的点赞", onOpenLikes)
                MenuDivider()
                MenuRow(NavIcons.Inbox, "我的下载", onOpenDownloads)
                MenuDivider()
                MenuRow(NavIcons.Note, "笔记本", onOpenNotes)
                MenuDivider()
                MenuRow(NavIcons.Todo, "待办", onOpenTodos)
            }

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceWashBrush())
            ) {
                MenuRow(NavIcons.Settings, "设置", onOpenSettings)
                MenuDivider()
                MenuRow(NavIcons.Logout, "退出登录", viewModel::logout, tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    // 登出后由上层清栈回首页；这里只在状态变化时触发一次
    LaunchedEffect(uiState.loggedOut) {
        if (uiState.loggedOut) onLoggedOut()
    }
}

@Composable
private fun ProfileHeader(
    nickname: String,
    avatar: String,
    bio: String,
    continueSignDay: Int,
    signedToday: Boolean,
    onOpenCheckin: () -> Unit,
    onOpenEditProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            // 个人资料卡：暖黄浅渐变，与便利贴的点缀色呼应
            .background(WarmYellowGradient)
            .padding(16.dp)
    ) {
        /*
         * 版式：头像 | 昵称 + 简介 | 每日签到 + 连续签到胶囊
         *
         * 两个必须遵守的点：
         *  1. 用 Row 权重分配宽度，**不要**用 Box + align 叠放 ——
         *     叠放时中列会独占整行宽度，简介文字直接压到右侧签到区上（实测重叠成一团）。
         *  2. 简介要 `fillMaxWidth()`。中列给 weight(1f) 只是「不抢别人的宽度」，
         *     本身仍会被签到列压窄；不给 fillMaxWidth 的话，简介会在被压窄的宽度上截断，
         *     显示成「热爱学习，正在补 A...」，白白浪费右侧空出来的位置。
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // 头像单独一列，自己垂直居中；昵称列不再被 64dp 头像挤窄
            Column {
                Avatar(url = avatar, name = nickname, size = 64.dp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nickname.ifBlank { "未登录" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 简介紧挨昵称，中间不留空行
                if (bio.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    MetaText(bio, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.width(8.dp))

            // 每日签到：与昵称同排、贴顶部
            Column(horizontalAlignment = Alignment.End) {
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenCheckin)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            NavIcons.Calendar,
                            contentDescription = "每日签到",
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "每日签到",
                            style = MaterialTheme.typography.labelMedium,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!signedToday) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                // 连续签到天数：白色胶囊，紧贴签到按钮下方。
                // 原来这里是纯文字「已连续 X 天」，而白胶囊留在昵称下方 —— 两条信息重复。
                // 现在把胶囊挪到签到下面顶替它，昵称下方只留简介。
                //
                // 内边距压到 8dp/2dp、字号用 labelSmall：这一列越宽，左边昵称+简介
                // 就被挤得越窄（实测 10dp 内边距时简介只剩 110dp，截成「正在补 A...」）。
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.66f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (continueSignDay > 0) "连续签到 $continueSignDay 天" else "今日未签到",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpenEditProfile)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                NavIcons.Edit,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "编辑主页",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    tint: Color = Primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (tint == Primary) TextPrimary else tint,
            modifier = Modifier.weight(1f)
        )
        Icon(
            NavIcons.ArrowForward,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MenuDivider() {
    Box(
        modifier = Modifier
            .padding(start = 48.dp, end = 16.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
