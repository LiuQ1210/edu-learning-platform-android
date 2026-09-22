package com.github.learningplatform.ui.nav

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.learningplatform.ui.theme.TabCommunityInk
import com.github.learningplatform.ui.theme.TabCommunityWash
import com.github.learningplatform.ui.theme.TabHomeInk
import com.github.learningplatform.ui.theme.TabHomeWash
import com.github.learningplatform.ui.theme.TabProfileInk
import com.github.learningplatform.ui.theme.TabProfileWash
import com.github.learningplatform.ui.theme.TabVideoInk
import com.github.learningplatform.ui.theme.TabVideoWash

/**
 * 底部导航项。
 *
 * 命名与数据库设计一致：
 *  - 第一个 Tab「首页」= course 模块入口（发现课程，非「我的课程」）
 *  - 第三个 Tab「社区」承载 article_* 系列表（book_* 已废弃）
 *  - 第四个 Tab「个人主页」= user 模块入口，包含签到入口
 *
 * 每个 Tab 有自己的 [ink]（选中图标/文字色）与 [wash]（选中垫底色），
 * 不再共用同一个主色 —— 主色蓝是给按钮和链接用的，压在奶油底上太跳。
 * 四组色都取自设计稿：藏青墨 #202040、青 #00B0C0、珊瑚 #D04030、暖黄 #F0B000，
 * 各配一格极淡的同色系垫底。
 */
enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    /** 选中态图标与文字颜色 */
    val ink: Color,
    /** 选中态垫底颜色 */
    val wash: Color
) {
    HOME(
        Routes.HOME, "首页", NavIcons.HomeOutline,
        TabHomeInk, TabHomeWash
    ),
    VIDEO(
        Routes.VIDEO, "视频", NavIcons.VideoOutline,
        TabVideoInk, TabVideoWash
    ),
    COMMUNITY(
        Routes.COMMUNITY, "社区", NavIcons.CommunityOutline,
        TabCommunityInk, TabCommunityWash
    ),
    PROFILE(
        Routes.PROFILE, "个人主页", NavIcons.ProfileOutline,
        TabProfileInk, TabProfileWash
    )
}
