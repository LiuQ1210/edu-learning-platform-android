package com.github.learningplatform.ui.nav

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航项。
 *
 * 每个 Tab 有自己的选中色，颜色在 AppNavHost 里根据深浅模式取。
 */
enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME(Routes.HOME, "首页", NavIcons.HomeOutline),
    VIDEO(Routes.VIDEO, "视频", NavIcons.VideoOutline),
    COMMUNITY(Routes.COMMUNITY, "社区", NavIcons.CommunityOutline),
    PROFILE(Routes.PROFILE, "个人主页", NavIcons.ProfileOutline);
}
