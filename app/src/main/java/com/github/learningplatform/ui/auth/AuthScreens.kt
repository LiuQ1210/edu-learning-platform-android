package com.github.learningplatform.ui.auth

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
import com.github.learningplatform.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.ui.common.PrimaryButton
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.surfaceWashBrush
import com.github.learningplatform.ui.theme.GlowOrb
import com.github.learningplatform.ui.theme.GradientBackground
import com.github.learningplatform.ui.theme.Surface
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/**
 * 登录页（对齐 UI 稿「登录」，去掉抖音/QQ 第三方入口）。
 *
 * 页面语义：3 个第三方入口只保留微信，其余账号体系走用户名密码。
 * 背景使用与 UI 稿一致的浅暖渐变，主色沿用品牌蓝。
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) onLoginSuccess()
    }

    AuthScaffold {
        Spacer(Modifier.height(48.dp))

        AuthBrand(title = "登录", subtitle = "欢迎使用，请登录以继续")

        Spacer(Modifier.height(10.dp))
        Text(
            // 原为「使用……账号登录」。「……」是当时没填上的占位符，去掉
            text = "使用账号登录",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        // 原此处有微信第三方登录入口，已按要求移除。
        // 间距从 24dp 收到 12dp：原来的 24dp 是给微信方块留的，
        // 去掉方块后这段空白达到 76px，而页面上其他元素间距都是 48px，
        // 明显比别处空。需要恢复微信入口时把这个值调回 24dp。
        Spacer(Modifier.height(12.dp))

        AuthField(
            value = uiState.username,
            onValueChange = viewModel::onUsernameChange,
            placeholder = "用户名",
            icon = NavIcons.Person,
            isError = uiState.usernameError != null,
            errorMessage = uiState.usernameError.orEmpty()
        )

        Spacer(Modifier.height(14.dp))

        AuthField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = "密码",
            icon = NavIcons.Lock,
            isPassword = true,
            isError = uiState.passwordError != null,
            errorMessage = uiState.passwordError.orEmpty()
        )

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = uiState.rememberMe,
                onCheckedChange = viewModel::onRememberMeChange,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "记住我",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "忘记密码",
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                modifier = Modifier.clickable { viewModel.notifyForgotPassword() }
            )
        }

        if (uiState.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = uiState.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            text = if (uiState.isLoading) "登录中..." else "登录",
            onClick = viewModel::login,
            enabled = !uiState.isLoading
        )

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text("还没有账号？", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                text = "立即注册",
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onNavigateToRegister)
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

// ---------------------------------------------------------------------------
// 登录 / 注册共用零件（RegisterScreen.kt 同包引用，故为 internal）
// ---------------------------------------------------------------------------

/**
 * 登录 / 注册页的统一承载：奶油渐变底 + 柔光圆 + 可滚动内容。
 *
 * 用 [GradientBackground] 对齐启动页的观感，登录页与注册页共享同一套配色，
 * 从启动页切过来不会出现「底色突然变了」的割裂。
 */
@Composable
internal fun AuthScaffold(content: @Composable () -> Unit) {
    GradientBackground(showGlow = false) {
        // 顶部柔光圆：设计稿的光晕集中在偏上区域
        GlowOrb(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            diameter = 300.dp,
            alpha = 0.85f
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

@Composable
internal fun AuthBrand(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            // 原为 72dp，视觉上偏大，压住了下面的「登录 / 注册」标题
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        // 用品牌图替换原来的「学」字块。图的底色与图标一致，
        // 所以不再额外铺 Primary 底，避免色块叠色。
        Image(
            painter = painterResource(R.drawable.brand_mark),
            contentDescription = null,
            // 用 Fit 而不是 Crop：这张图近似正方形，Crop 会把右上角的播放键小图标切掉
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
    Spacer(Modifier.height(12.dp))
    Text(
        text = title,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )
}

/** 圆角输入框（与 UI 稿的浅色胶囊输入框一致） */
@Composable
internal fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    keyboardType: androidx.compose.ui.text.input.KeyboardType =
        androidx.compose.ui.text.input.KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    var visible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(50))
                .background(surfaceWashBrush())
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = if (isPassword && !visible) PasswordVisualTransformation()
                else androidx.compose.ui.text.input.VisualTransformation.None,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = if (isPassword) {
                        androidx.compose.ui.text.input.KeyboardType.Password
                    } else {
                        keyboardType
                    }
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Primary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = TextHint, style = MaterialTheme.typography.bodyLarge)
                    }
                    inner()
                }
            )
            if (isPassword) {
                IconButton(onClick = { visible = !visible }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (visible) NavIcons.VisibilityOff else NavIcons.Visibility,
                        contentDescription = if (visible) "隐藏密码" else "显示密码",
                        tint = TextHint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (isError && errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 18.dp, top = 4.dp)
            )
        }
    }
}
