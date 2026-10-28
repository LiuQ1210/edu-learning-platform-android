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
import com.github.learningplatform.ui.theme.TextHint
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
    onOpenNotifications: () -> Unit,
    onOpenCheckin: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenCommunityProfile: () -> Unit,
    onOpenMyCourses: () -> Unit,
    onOpenStudyStats: () -> Unit,
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenNotifications)
                    .padding(4.dp)
            ) {
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
                sexLabel = uiState.sexLabel,
                followCount = uiState.followCount,
                fansCount = uiState.fansCount,
                likeReceivedCount = uiState.likeReceivedCount,
                continueSignDay = uiState.continueSignDay,
                signedToday = uiState.signedToday,
                onOpenCheckin = onOpenCheckin,
                onOpenCommunityProfile = onOpenCommunityProfile,
                onOpenEditProfile = onOpenEditProfile
            )

            /*
             * 加载态。
             *
             * 为什么只给资料区加提示、不用整页 LoadingState：
             * 下面的菜单是纯静态入口，加载资料期间也能点进「我的课程」「设置」等页面，
             * 整页 loading 会把它们一起遮住，反而降低可用性。
             *
             * 判据用「昵称为空」而不是只看 isLoading：已有缓存资料时刷新不该再闪一下加载提示。
             */
            if (uiState.isLoading && uiState.nickname.isBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "正在加载资料…",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint
                )
            }

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
                MenuDivider()
                MenuRow(NavIcons.Check, "任务中心", onOpenCheckin)
                MenuDivider()
                MenuRow(NavIcons.Calendar, "学习数据", onOpenStudyStats)
                MenuDivider()
                                MenuRow(NavIcons.History, "观看历史", onOpenHistory)
                MenuDivider()
                MenuRow(NavIcons.Star, "我的收藏", onOpenFavorites)
                MenuDivider()
                MenuRow(NavIcons.Favorite, "我的点赞", onOpenLikes)
                MenuDivider()
                MenuDivider()
                MenuRow(NavIcons.Note, "笔记本", onOpenNotes)
                MenuDivider()
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
    sexLabel: String,
    followCount: Int,
    fansCount: Int,
    likeReceivedCount: Int,
    continueSignDay: Int,
    signedToday: Boolean,
    onOpenCheckin: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenCommunityProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenCommunityProfile)
            .background(WarmYellowGradient)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像 + 粉丝数（真实值来自 /api/v1/user/home 的 fansCount）
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(url = avatar, name = nickname, size = 64.dp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "$fansCount 粉丝",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
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
                if (bio.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    MetaText(bio, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(2.dp))
                // 关注 / 获赞 也都是 /user/home 返回的真实值；性别用后端 sexLabel
                val stats = buildList {
                    if (sexLabel.isNotBlank()) add(sexLabel)
                    add("关注 $followCount")
                    add("获赞 $likeReceivedCount")
                }.joinToString(" · ")
                MetaText(stats, modifier = Modifier.fillMaxWidth())
            }
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
