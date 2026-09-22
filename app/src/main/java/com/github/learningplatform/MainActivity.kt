package com.github.learningplatform

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.github.learningplatform.ui.nav.AppNavHost
import com.github.learningplatform.ui.theme.LearnPlatformTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 系统启动窗口的驻留时间。
 *
 * 与 Compose 启动页（[com.github.learningplatform.ui.nav.SplashScreen]）的
 * `minDurationMs` 取同一个值，让「白底 + 品牌图标」直接接到「奶油渐变 + 品牌名」，
 * 中间不出现空白帧。
 */
private const val SPLASH_MIN_MS = 700L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启动窗口（主题 Theme.LearnPlatform.Splash）先顶上，压掉冷启动的默认背景与默认图标。
        // 条件成立期间它保持可见，条件一撤即切到 postSplashScreenTheme。
        val startedAt = SystemClock.uptimeMillis()
        installSplashScreen().setKeepOnScreenCondition {
            SystemClock.uptimeMillis() - startedAt < SPLASH_MIN_MS
        }

        enableEdgeToEdge()
        setContent {
            LearnPlatformTheme {
                AppNavHost()
            }
        }
    }
}
