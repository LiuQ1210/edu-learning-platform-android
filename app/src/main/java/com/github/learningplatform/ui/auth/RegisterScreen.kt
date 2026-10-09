package com.github.learningplatform.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.ui.common.PrimaryButton
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.TextSecondary

/**
 * 注册页（【登录注册联调】按若依 RegisterDTO 适配）。
 *
 * 若依注册无短信/邮箱验证码通道，账号为 username（用户名/手机号），
 * 验证码为**图形验证码**（uuid + 图片，点击图片刷新）。
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.registerSuccess) {
        if (uiState.registerSuccess) onRegisterSuccess()
    }

    AuthScaffold {
        Spacer(Modifier.height(36.dp))

        AuthBrand(title = "注册", subtitle = "欢迎！请注册后继续")

        Spacer(Modifier.height(10.dp))
        Text(
            text = "使用用户名/手机号注册",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(Modifier.height(16.dp))

        AuthField(
            value = uiState.username,
            onValueChange = viewModel::onUsernameChange,
            placeholder = "用户名/手机号",
            icon = NavIcons.Person,
            keyboardType = KeyboardType.Text,
            isError = uiState.usernameError != null,
            errorMessage = uiState.usernameError.orEmpty()
        )
        Spacer(Modifier.height(12.dp))

        // 【登录注册联调】若依图形验证码：输入 + 图片（点击图片刷新）
        CaptchaField(
            imageBase64 = uiState.captchaImg,
            codeValue = uiState.captchaCode,
            onCodeChange = viewModel::onCaptchaCodeChange,
            onRefresh = viewModel::loadCaptcha,
            loading = uiState.captchaLoading,
            isError = uiState.captchaError != null,
            errorMessage = uiState.captchaError.orEmpty()
        )
        Spacer(Modifier.height(12.dp))

        AuthField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = "密码（6-20 位，含字母和数字）",
            icon = NavIcons.Lock,
            isPassword = true,
            isError = uiState.passwordError != null,
            errorMessage = uiState.passwordError.orEmpty()
        )
        Spacer(Modifier.height(12.dp))

        AuthField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            placeholder = "确认密码",
            icon = NavIcons.Lock,
            isPassword = true,
            isError = uiState.confirmError != null,
            errorMessage = uiState.confirmError.orEmpty()
        )
        Spacer(Modifier.height(12.dp))

        AuthField(
            value = uiState.nickname,
            onValueChange = viewModel::onNicknameChange,
            placeholder = "昵称（选填，不填自动生成）",
            icon = NavIcons.Person
        )

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = uiState.agreed,
                onCheckedChange = viewModel::onAgreedChange,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("我已阅读并同意 ", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                text = "服务条款",
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                modifier = Modifier.clickable { }
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

        Spacer(Modifier.height(18.dp))

        PrimaryButton(
            text = if (uiState.isLoading) "注册中..." else "注册",
            onClick = viewModel::register,
            enabled = !uiState.isLoading
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text("已有账号？", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                text = "立即登录",
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onNavigateToLogin)
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}
