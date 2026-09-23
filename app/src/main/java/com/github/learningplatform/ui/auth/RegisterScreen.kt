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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.ui.common.PrimaryButton
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.surfaceWashBrush
import com.github.learningplatform.ui.theme.Surface
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/**
 * 注册页（接口文档 v3.1 增补 2.2：手机号 / 邮箱双通道）。
 *
 * 与 UI 稿的差异及原因：
 *  - UI 稿只有「用户名 / 邮箱 / 密码 / 确认密码」，但 2.2 的账号标识是手机号或邮箱，
 *    且验证码是必填项，所以这里按通道切换账号输入框并补上验证码 + 发送按钮。
 *  - 昵称改到后端自动生成（选填留空即可），不再单独占一个必填输入框。
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
            // 原为「使用……」+ 渠道 + 「注册」。「……」是占位符，去掉
            text = "使用" + uiState.channel.label + "注册",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(Modifier.height(16.dp))

        // 通道切换：手机号 / 邮箱
        ChannelSwitcher(
            selected = uiState.channel,
            onSelect = viewModel::onChannelChange
        )

        Spacer(Modifier.height(16.dp))

        AuthField(
            value = uiState.account,
            onValueChange = viewModel::onAccountChange,
            placeholder = uiState.channel.label,
            icon = if (uiState.channel == RegisterChannel.PHONE) NavIcons.Person else NavIcons.Email,
            keyboardType = if (uiState.channel == RegisterChannel.PHONE) KeyboardType.Phone
            else KeyboardType.Email,
            isError = uiState.accountError != null,
            errorMessage = uiState.accountError.orEmpty()
        )
        Spacer(Modifier.height(12.dp))

        // 验证码 + 发送按钮（60 秒倒计时）
        AuthCodeField(
            value = uiState.code,
            onValueChange = viewModel::onCodeChange,
            onSendCode = viewModel::sendCode,
            sending = uiState.sendingCode,
            countdown = uiState.countdown,
            sendEnabled = uiState.account.isNotBlank(),
            isError = uiState.codeError != null,
            errorMessage = uiState.codeError.orEmpty()
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

        if (uiState.message != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = uiState.message.orEmpty(),
                color = Primary,
                style = MaterialTheme.typography.bodySmall
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

/** 注册通道切换：手机号 / 邮箱 */
@Composable
private fun ChannelSwitcher(
    selected: RegisterChannel,
    onSelect: (RegisterChannel) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(surfaceWashBrush())
            .padding(4.dp)
    ) {
        RegisterChannel.entries.forEach { channel ->
            val active = channel == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Primary else Color.Transparent)
                    .clickable { onSelect(channel) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (active) Color.White else TextSecondary,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/** 验证码输入框：右侧内嵌「获取验证码 / 倒计时」 */
@Composable
private fun AuthCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    onSendCode: () -> Unit,
    sending: Boolean,
    countdown: Int,
    sendEnabled: Boolean,
    isError: Boolean,
    errorMessage: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(50))
                .background(surfaceWashBrush())
                .padding(start = 18.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                NavIcons.Lock,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = { input -> onValueChange(input.filter { it.isDigit() }.take(6)) },
                singleLine = true,
                cursorBrush = SolidColor(Primary),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text("验证码", color = TextHint, style = MaterialTheme.typography.bodyLarge)
                    }
                    inner()
                }
            )

            val canSend = sendEnabled && countdown == 0 && !sending
            Text(
                text = when {
                    sending -> "发送中"
                    countdown > 0 -> "${countdown}s 后重发"
                    else -> "获取验证码"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (canSend) Primary else TextHint,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = canSend, onClick = onSendCode)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
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
