package com.github.learningplatform.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * 浅色配色。
 *
 * **必须把 Material 3 的角色填全**：`lightColorScheme()` 只传几个参数时，
 * 未传的角色会退回库自带的紫色基线。之前的症状就是 —— 底栏 `NavigationBarItem`
 * 取 `secondaryContainer` 当选中胶囊、取 `onSecondaryContainer` 当选中图标/文字色，
 * 于是选中态显示成「淡紫底 + 橙字」，与设计稿的蓝色不一致。
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,

    secondary = Secondary,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,

    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,

    // 这几个「表面容器」角色最容易被忽略，而底栏 / 输入框 / 卡片底色都取它们
    surfaceTint = Primary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FAFC),
    surfaceContainer = Color(0xFFF1F3F7),
    surfaceContainerHigh = Color(0xFFEBEEF3),
    surfaceContainerHighest = Color(0xFFE5E9EF),

    inverseSurface = Color(0xFF2E3134),
    inverseOnSurface = Color(0xFFF1F3F7),
    inversePrimary = Color(0xFF9EC3FF),

    outline = Outline,
    outlineVariant = OutlineVariant,

    error = ErrorColor,
    onError = OnErrorColor,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    scrim = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = OnPrimaryContainer,
    onPrimaryContainer = PrimaryContainer,

    secondary = Color(0xFF7BD48C),
    onSecondary = Color(0xFF00391A),
    secondaryContainer = Color(0xFF0B5227),
    onSecondaryContainer = SecondaryContainer,

    tertiary = Color(0xFF9BCBFF),
    onTertiary = Color(0xFF003353),
    tertiaryContainer = Color(0xFF004A75),
    onTertiaryContainer = TertiaryContainer,

    background = DarkBackground,
    onBackground = Color(0xFFE3E3E3),
    surface = DarkSurface,
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC4C7CB),

    surfaceTint = Primary,
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF191A1C),
    surfaceContainer = Color(0xFF1D1E20),
    surfaceContainerHigh = Color(0xFF282A2C),
    surfaceContainerHighest = Color(0xFF333537),

    inverseSurface = Color(0xFFE3E3E3),
    inverseOnSurface = Color(0xFF2E3134),
    inversePrimary = PrimaryDark,

    outline = Color(0xFF8E9196),
    outlineVariant = DarkOutlineVariant,

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    scrim = Color.Black
)

/**
 * @param dynamicColor 默认关闭。开启后 Android 12+ 会用壁纸动态取色覆盖品牌色，
 *                     教育类产品通常需要稳定的品牌色，故默认 false。
 *
 * 说明：不再手动设置 statusBarColor —— Activity 已调用 enableEdgeToEdge()，
 * 该 API 在 edge-to-edge 下已废弃且不生效。
 */
@Composable
fun LearnPlatformTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
