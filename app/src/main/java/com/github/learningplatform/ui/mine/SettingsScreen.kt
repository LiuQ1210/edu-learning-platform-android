package com.github.learningplatform.ui.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.BuildConfig
import com.github.learningplatform.data.local.ThemeMode
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.CommonTextField
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.surfaceWashBrush
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Surface as SurfaceColor
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/** 设置（对齐 UI 稿底部独立分组） */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAgreement by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showClearCache by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.loggedOut) {
        if (uiState.loggedOut) onLoggedOut()
    }
    // 清除缓存后弹个轻提示
    LaunchedEffect(uiState.cleared) {
        if (uiState.cleared) {
            // 复用 snackbar 太重，这里直接 Toast 一下即可
            android.widget.Toast.makeText(context, "缓存已清除", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeCleared()
        }
    }

    Scaffold(
        topBar = { BackTopBar(title = "设置", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(pageGradientBrush(strong = true))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceWashBrush())
            ) {
                SettingRow(NavIcons.Lock, "修改密码", onClick = onOpenChangePassword)
                Divider()
                SettingRow(
                    icon = NavIcons.Bookmark,
                    title = "主题模式",
                    trailing = uiState.themeMode.label(),
                    onClick = { showThemeDialog = true }
                )
            }

            Spacer(Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceWashBrush())
            ) {
                SettingRow(NavIcons.Inbox, "清除缓存", trailing = uiState.cacheSizeLabel, onClick = { showClearCache = true })
                Divider()
                SettingRow(NavIcons.Note, "用户协议", onClick = { showAgreement = true })
                Divider()
                SettingRow(NavIcons.Bookmark, "隐私政策", onClick = { showPrivacy = true })
                Divider()
                SettingRow(NavIcons.Community, "关于我们", trailing = "v" + BuildConfig.VERSION_NAME, onClick = { showAbout = true })
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(surfaceWashBrush())
                    .clickable { showLogoutDialog = true }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "退出登录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "学习平台 " + BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("退出后需要重新登录才能继续学习，确定吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) { Text("退出", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("主题模式") },
            text = {
                Column {
                    ThemeOption(
                        label = ThemeMode.LIGHT.label(),
                        selected = uiState.themeMode == ThemeMode.LIGHT,
                        onClick = {
                            viewModel.setThemeMode(ThemeMode.LIGHT)
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        label = ThemeMode.DARK.label(),
                        selected = uiState.themeMode == ThemeMode.DARK,
                        onClick = {
                            viewModel.setThemeMode(ThemeMode.DARK)
                            showThemeDialog = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("关闭") }
            }
        )
    }

    if (showAgreement) {
        InfoDialog(
            title = "用户协议",
            text = "欢迎使用本学习平台。使用本应用即表示您同意：\n\n1. 您应妥善保管账号，因账号泄露造成的损失由您自行承担；\n2. 平台内容仅供个人学习使用，未经许可不得商用或二次分发；\n3. 我们有权根据运营需要调整课程内容与功能，重要变更会提前通知。",
            onDismiss = { showAgreement = false }
        )
    }

    if (showPrivacy) {
        InfoDialog(
            title = "隐私政策",
            text = "我们重视您的隐私：\n\n1. 仅收集学习行为（观看历史、进度、点赞收藏）用于改进推荐与记录学习进度；\n2. 不会向第三方共享您的个人身份信息；\n3. 您可在「设置-清除缓存」中清除本地缓存数据。",
            onDismiss = { showPrivacy = false }
        )
    }

    if (showAbout) {
        InfoDialog(
            title = "关于我们",
            text = "学习平台 v" + BuildConfig.VERSION_NAME + "\n\n面向学习者的综合学习社区，提供课程、文章与短视频学习内容。",
            onDismiss = { showAbout = false }
        )
    }

    if (showClearCache) {
        AlertDialog(
            onDismissRequest = { showClearCache = false },
            title = { Text("清除缓存") },
            text = { Text("将清除本地图片与临时缓存（不影响学习记录），当前 " + uiState.cacheSizeLabel + "，确定清除吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showClearCache = false
                    viewModel.clearCache()
                }) { Text("清除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCache = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun InfoDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    )
}
@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(NavIcons.Bookmark, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    trailing: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.width(6.dp))
        }
        Icon(
            NavIcons.ArrowForward,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .padding(start = 48.dp, end = 16.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

/**
 * 修改密码（接口文档 2.10 POST auth/change-password）。
 *
 * 修改成功后后端会作废 refreshToken，因此这里强制回登录页（onDone -> 上级清栈）。
 */
@Composable
fun ChangePasswordScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: ChangePasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.done) {
        if (uiState.done) onDone()
    }

    Scaffold(
        topBar = { BackTopBar(title = "修改密码", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(pageGradientBrush(strong = true))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            CommonTextField(
                value = uiState.oldPassword,
                onValueChange = viewModel::onOldPasswordChange,
                label = "当前密码",
                placeholder = "请输入当前密码",
                isError = uiState.error != null
            )
            Spacer(Modifier.height(16.dp))
            CommonTextField(
                value = uiState.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                label = "新密码",
                placeholder = "6-20 位，建议字母+数字",
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            Spacer(Modifier.height(16.dp))
            CommonTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = "确认新密码",
                placeholder = "请再次输入新密码",
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                isError = uiState.error != null,
                errorMessage = uiState.error.orEmpty()
            )

            Spacer(Modifier.height(28.dp))
            androidx.compose.material3.Button(
                onClick = viewModel::submit,
                enabled = !uiState.submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(if (uiState.submitting) "提交中..." else "确认修改")
            }
        }
    }
}