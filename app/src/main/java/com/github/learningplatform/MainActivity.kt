package com.github.learningplatform

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.local.ThemeMode
import com.github.learningplatform.data.local.UserPreferences
import com.github.learningplatform.ui.nav.AppNavHost
import com.github.learningplatform.ui.theme.LearnPlatformTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

    @Inject lateinit var userPreferences: UserPreferences

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
            // 设置页改主题后 DataStore 会推新值，这里重组即切主题，不需要重启 Activity。
            // 冷启动时给个默认值 SYSTEM，第一帧后由 collect 覆盖。
            val themeMode = userPreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT).value
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> false
            }
            LearnPlatformTheme(darkTheme = darkTheme) {
                AppNavHost()
            }
        }
    }
}
