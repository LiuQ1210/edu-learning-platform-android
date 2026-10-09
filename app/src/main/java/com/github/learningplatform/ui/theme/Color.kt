package com.github.learningplatform.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// 品牌色（取自设计稿实采样，启动页 750x1624）
//
// 品牌蓝 #3677EF 是从大标题「学习平台」与底部 logo 文字上直接采到的饱和色，
// 比通用 Google 蓝 #1A73E8 更亮、更偏紫，和插画里的蓝（#4080F0）能对上。
// ---------------------------------------------------------------------------

/** 品牌主色 */
val Primary = Color(0xFF3677EF)

/** 主色深版：渐变末端、按压态 */
val PrimaryDark = Color(0xFF2A5FD0)

/** 主色浅版：选中态胶囊底、徽标底 */
val PrimaryContainer = Color(0xFFDCE8FF)
val OnPrimaryContainer = Color(0xFF0C2C6B)

/** 插画蓝：比主色更亮一档，渐变起点与强调用 */
val BrandBlueLight = Color(0xFF5B9BFF)

/** 深海军蓝：取自底部 logo 暗部，高对比文字/图标用 */
val BrandNavy = Color(0xFF282840)

// ---------------------------------------------------------------------------
// 插画点缀色（设计稿里的便利贴与椅子垫）
// 给浅色界面提供色彩层次：状态标签、优先级、徽标
// ---------------------------------------------------------------------------

/** 暖黄：便利贴 */
val AccentYellow = Color(0xFFF0B000)

/** 珊瑚红：便利贴、告警 */
val AccentCoral = Color(0xFFD04030)

/** 柔粉：椅子垫，「已收藏」这类柔性状态 */
val AccentPink = Color(0xFFF0B0B0)

/** 青绿：取自底图右下与 logo 高光，「已完成」「已发布」 */
val AccentTeal = Color(0xFF00B0C0)

// ---------------------------------------------------------------------------
// 浅色渐变停靠点：设计稿是「暖白 → 杏 → 粉 → 白」四段，不是均匀过渡。
// 全是实采值，别随手改，否则会丢掉那种奶油底的观感。
// ---------------------------------------------------------------------------

/** 顶部：接近纯白，略暖 */
val GradTop = Color(0xFFFFFFFF)

/** 上段：极淡暖奶油 */
val GradCream = Color(0xFFFFFBF2)

/** 中段：杏色偏粉（设计稿 34%-40% 区间） */
val GradPeach = Color(0xFFF8F0ED)

/** 下段：淡粉 */
val GradBlush = Color(0xFFFDF5F5)

/** 收尾：回到纯白 */
val GradBottom = Color(0xFFFFFFFF)

/** 柔光圆心：设计稿中央那个大圆光晕 */
val GlowCenter = Color(0xFFFFFFFF)

/** 柔光边缘：向外过渡到的淡灰蓝，做出「磨砂玻璃」层次 */
val GlowEdge = Color(0xFFDCE5EF)

// ---------------------------------------------------------------------------
// 向后兼容别名（老代码按语义名引用）
// ---------------------------------------------------------------------------

val Secondary = AccentTeal
val SecondaryContainer = Color(0xFFD3F2F5)
val OnSecondaryContainer = Color(0xFF003D44)

val Tertiary = AccentYellow
val TertiaryContainer = Color(0xFFFFEFC7)
val OnTertiaryContainer = Color(0xFF4A3300)

// ---------------------------------------------------------------------------
// 浅色背景 / 表面：白卡 + 极浅灰蓝底
// ---------------------------------------------------------------------------

val Background = Color(0xFFF7F9FC)
val OnBackground = Color(0xFF1A1A1A)
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF1A1A1A)
val SurfaceVariant = Color(0xFFF0F3F8)
val OnSurfaceVariant = Color(0xFF5F6368)

val Outline = Color(0xFFDADCE0)
val OutlineVariant = Color(0xFFEEEEEE)

val ErrorColor = Color(0xFFD04030)
val OnErrorColor = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFDE0DF)
val OnErrorContainer = Color(0xFF7F1D1B)

// ---------------------------------------------------------------------------
// 深色模式配色（另外一套，不动浅色值）。
//
// 选深蓝灰调而不是纯灰黑：品牌色是蓝 #3677EF，深色底带一点蓝调
// 才能和品牌色呼应，纯黑 #000 / 纯灰 #121212 会和蓝色按钮割裂。
// 明度层级：背景最暗，卡片亮一档，分隔线再亮一档，保证层级可辨。
// ---------------------------------------------------------------------------

/** 深色页面底：深蓝灰，不是纯黑 */
val DarkBackground = Color(0xFF151A26)
val DarkSurface = Color(0xFF1E2536)
val DarkSurfaceVariant = Color(0xFF28304A)
val DarkOutline = Color(0xFF353B4D)
val DarkOutlineVariant = Color(0xFF2A3040)

/** 深色下的文字色：不纯白，带一点冷灰，和深蓝底协调 */
val DarkTextPrimary = Color(0xFFE8EAF0)
val DarkTextSecondary = Color(0xFF9AA3B5)
val DarkTextHint = Color(0xFF6B7385)

// ---------------------------------------------------------------------------
// 语义色：跟随主题选择。
//
// 浅色分支的值和上面浅色块里定义的完全一致，没动；
// 深色分支用的是新加的 Dark* 色值。调用方写 `color = TextPrimary`，
// 浅色下拿到 OnSurface，深色下拿到 DarkTextPrimary。
// ---------------------------------------------------------------------------

val TextPrimary: Color
    @Composable @ReadOnlyComposable get() =
        if (LocalAppDarkTheme.current) DarkTextPrimary else OnSurface

val TextSecondary: Color
    @Composable @ReadOnlyComposable get() =
        if (LocalAppDarkTheme.current) DarkTextSecondary else Color(0xFF666666)

val TextHint: Color
    @Composable @ReadOnlyComposable get() =
        if (LocalAppDarkTheme.current) DarkTextHint else Color(0xFF999999)

val Divider: Color
    @Composable @ReadOnlyComposable get() =
        if (LocalAppDarkTheme.current) DarkOutlineVariant else OutlineVariant

val Error = ErrorColor
val Success = AccentTeal

/** 底栏容器：浅色纯白，深色用深蓝灰 surface */
val BottomNavContainer: Color
    @Composable @ReadOnlyComposable get() =
        if (LocalAppDarkTheme.current) DarkSurface else Color(0xFFFFFFFF)

// ---------------------------------------------------------------------------
// 底栏
//
// 配色依据（都是设计稿实采值，不是随手挑的）：
//   深藏青 #202040 是设计稿里出现最多的深色（墨水色，色相 240）；
//   柔粉 #F0B0B0 是椅子垫；主色 #3677EF 是大标题/logo 的蓝。
//
// 选中态刻意**不用**高饱和的主色蓝 —— 那是给按钮和链接用的，压在奶油底上太跳。
// 改用设计稿的墨水色做图标/文字，配一格极淡的暖色垫底，和整页的奶油调统一。
// ---------------------------------------------------------------------------

/** 未选中：中性灰，带一点暖调，不要用冷灰 */
val BottomNavUnselected = Color(0xFF9A9AA3)

/** 首页：藏青墨 + 淡蓝垫 */
val TabHomeInk = BrandNavy
val TabHomeWash = Color(0xFFE3E8FB)

/** 视频：深青墨 + 淡青垫 */
val TabVideoInk = Color(0xFF0E6E78)
val TabVideoWash = Color(0xFFD9F0F3)

/** 社区：砖红墨 + 淡粉垫（呼应设计稿的珊瑚红与椅子垫粉） */
val TabCommunityInk = Color(0xFFA8453C)
val TabCommunityWash = Color(0xFFFBE4E0)

/** 个人主页：赭金墨 + 淡黄垫（呼应便利贴暖黄） */
val TabProfileInk = Color(0xFF96650C)
val TabProfileWash = Color(0xFFFBEFD2)

/** 兼容旧引用 */
val BottomNavSelected = TabHomeInk
val BottomNavIndicator = TabHomeWash
