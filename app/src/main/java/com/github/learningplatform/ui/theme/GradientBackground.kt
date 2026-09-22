package com.github.learningplatform.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 奶油渐变背景。
 *
 * 对齐设计稿：整体是自上而下的四段竖向渐变（暖白 → 杏 → 粉 → 白），
 * 中间再叠一个巨大的柔光圆，做出「磨砂玻璃」的层次。
 *
 * 为什么不用两端插值的 `verticalGradient(colorA, colorB)`：设计稿的过渡并不均匀 ——
 * 顶部 0-22% 几乎全白，34%-40% 才到最深的杏粉，之后又慢慢泛白。
 * 用四个明确停靠点比两端插值更接近原稿。
 *
 * @param showGlow 是否叠柔光圆。底部要贴内容列表的页面建议关掉，省一层绘制
 * @param glowCenterY 柔光圆心的纵向位置比例，0.42 ≈ 设计稿
 * @param glowRadiusRatio 柔光半径相对屏幕宽度的比例
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    showGlow: Boolean = true,
    glowCenterY: Float = 0.42f,
    glowRadiusRatio: Float = 1.15f,
    content: @Composable BoxScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.00f to GradTop,
                    0.22f to GradCream,
                    0.38f to GradPeach,
                    0.72f to GradBlush,
                    1.00f to GradBottom
                )
            )
    ) {
        if (showGlow) {
            // 柔光圆：径向渐变，圆心按容器尺寸算，半径取屏宽的比例。
            // 不用固定像素值 —— 不同屏幕密度下会差很多。
            val density = androidx.compose.ui.platform.LocalDensity.current
            val centerXPx = with(density) { maxWidth.toPx() } / 2f
            val centerYPx = with(density) { maxHeight.toPx() } * glowCenterY
            val radiusPx = with(density) { maxWidth.toPx() } * glowRadiusRatio
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                GlowCenter.copy(alpha = 0.95f),
                                GlowCenter.copy(alpha = 0.60f),
                                GlowEdge.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(centerXPx, centerYPx),
                            radius = radiusPx
                        )
                    )
            )
        }
        content()
    }
}

/**
 * 柔光圆斑：给局部区域（登录页顶部、空状态插画后面）加一层光晕。
 * 单独抽出来是因为小尺寸径向渐变比整屏叠加便宜得多。
 */
@Composable
fun GlowOrb(
    modifier: Modifier = Modifier,
    diameter: Dp = 260.dp,
    alpha: Float = 0.85f
) {
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha),
                        GlowEdge.copy(alpha = alpha * 0.30f),
                        Color.Transparent
                    )
                )
            )
            .blur(32.dp)
    )
}

/**
 * 可点元素表面的轻柔渐变（卡片、胶囊、列表行）。
 *
 * 强度刻意压得很低 —— 目的是让这些元素融入奶油底色，而不是自己被看见。
 * 直接用 `Brush.linearGradient` 线性过渡，不用径向，避免在扁长卡片上出现光斑。
 *
 * @param selected 选中态：换成品牌浅蓝到白的过渡，和未选中的暖色形成区分
 */
@Composable
fun surfaceWashBrush(selected: Boolean = false): Brush = if (selected) {
    Brush.linearGradient(listOf(PrimaryContainer.copy(alpha = 0.70f), Surface))
} else {
    Brush.linearGradient(listOf(SurfaceWashStart, SurfaceWashEnd))
}

/** 轻柔渐变的两个端点，单独导出以便需要纯色时取其一 */
val SurfaceWashStart: Color get() = Color(0xFFFFFCF7)
val SurfaceWashEnd: Color get() = Color(0xFFFEF8F4)

/**
 * 页面背景渐变（Brush 版）。
 *
 * 与 [GradientBackground] 的区别：这个只返回 Brush，不改布局结构 ——
 * 直接替换根节点上原来的 `.background(纯色)` 即可：
 *
 * ```
 * // 改前
 * Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
 * // 改后
 * Modifier.fillMaxSize().background(pageGradientBrush())
 * ```
 *
 * 这样改的好处是**不引入新的嵌套层级、不改花括号配对**，逐个文件替换不会
 * 把布局改坏；渐变方向与停靠点和设计稿一致。
 *
 * @param strong 是否用完整四段。true 用于内容较少的页面（详情、列表空态）；
 *               false 用于长列表页，顶部奶白往下很快回到中性底，避免长时间阅读疲劳
 */
fun pageGradientBrush(strong: Boolean = true): Brush = if (strong) {
    Brush.verticalGradient(
        0.00f to GradTop,
        0.12f to GradCream,
        0.34f to GradPeach,
        0.72f to GradBlush,
        1.00f to GradBottom
    )
} else {
    Brush.verticalGradient(
        0.00f to GradPeach,
        0.10f to GradCream,
        0.28f to Background,
        1.00f to Background
    )
}

// ---------------------------------------------------------------------------
// 彩色浅渐变：给成块的容器（头像卡、统计块、按钮）一点色彩倾向
//
// 都保持「浅到不抢内容」的强度：起点比终点略深一点，跨度控制在 6-10 个色阶内。
// 设计稿的点缀色各自对应一支，取色沿用 Color.kt 里的实采值。
// ---------------------------------------------------------------------------

/**
 * 暖黄：个人资料卡、签到相关
 *
 * 起点原为 #FFF3D6，实机看着偏重、把卡片压得发"土"。现在把黄往淡里压一档
 * （#FFF8E8 比原来淡约 40%），终点几乎归到白，整体只剩一层若有若无的暖调。
 */
val WarmYellowGradient: Brush
    get() = Brush.linearGradient(listOf(Color(0xFFFFF8E8), Color(0xFFFFFDF8)))

/** 淡青：视频、统计类 */
val SoftTealGradient: Brush
    get() = Brush.linearGradient(listOf(Color(0xFFDCF2F5), Color(0xFFF4FBFC)))

/** 淡粉：社区、互动类 */
val SoftPinkGradient: Brush
    get() = Brush.linearGradient(listOf(Color(0xFFFDE7E3), Color(0xFFFFF7F5)))

/** 淡蓝：品牌、课程类 */
val SoftBlueGradient: Brush
    get() = Brush.linearGradient(listOf(Color(0xFFE3ECFD), Color(0xFFF6F9FF)))

/** 主色渐变：主按钮、选中胶囊等需要质感的元素 */
val BrandGradient: Brush
    get() = Brush.linearGradient(listOf(BrandBlueLight, Primary))

/** 主色横向渐变：进度条 / 指标条 */
val BrandGradientHorizontal: Brush
    get() = Brush.horizontalGradient(listOf(BrandBlueLight, Primary))
