package com.github.learningplatform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.local.ThemeMode
import com.github.learningplatform.data.local.UserPreferences
import com.github.learningplatform.ui.common.PrivacyConsentGate
import com.github.learningplatform.ui.nav.AppNavHost
import com.github.learningplatform.ui.theme.LearnPlatformTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 系统启动窗口（Theme.LearnPlatform.Splash）只负责冷启动到首帧这段，
        // 首帧画出 Compose 启动页即交接，不再额外驻留 —— 驻留越久越像「两个启动页」。
        installSplashScreen()

        enableEdgeToEdge()

        // 首帧之前同步读一次主题：DataStore 首次读取有异步延迟，
        // 若用 LIGHT 当 initialValue，深色用户启动时 Compose 会先用浅色画一帧启动页再闪切深色。
        // 文件只有几百字节，读取在系统启动屏遮盖下完成，不增加可感知耗时。
        val initialTheme = runBlocking { userPreferences.themeMode.first() }

        setContent {
            // 设置页改主题后 DataStore 会推新值，这里重组即切主题，不需要重启 Activity。
            // 只有浅色/深色两态，默认浅色，不跟随系统。
            val themeMode = userPreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = initialTheme).value
            LearnPlatformTheme(darkTheme = themeMode == ThemeMode.DARK) {
                PrivacyConsentGate {
                    AppNavHost()
                }
            }
        }
    }
}
