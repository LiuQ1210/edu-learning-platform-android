package com.github.learningplatform.ui.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.ui.common.Avatar
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.CommonTextField
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Surface as SurfaceColor
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/**
 * 编辑个人资料（接口文档 3.2 PUT user/profile）。
 *
 * 头像同样受「文件上传接口未定义」限制，只能填图片直链。
 * 昵称 / 性别 / 简介可改，改完立即回写 DataStore 以便个人主页首屏复用。
 */
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { BackTopBar(title = "编辑主页", onBack = onBack) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = viewModel::save,
                    enabled = !uiState.saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(if (uiState.saving) "保存中..." else "保存")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(pageGradientBrush(strong = true))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(url = uiState.avatar, name = uiState.nickname, size = 64.dp)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "头像",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "填写图片直链即可更换",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHint
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            CommonTextField(
                value = uiState.avatar,
                onValueChange = viewModel::onAvatarChange,
                label = "头像地址",
                placeholder = "https://..."
            )

            Spacer(Modifier.height(16.dp))
            CommonTextField(
                value = uiState.nickname,
                onValueChange = viewModel::onNicknameChange,
                label = "昵称",
                placeholder = "请输入昵称"
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "性别",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0 to "不公开", 1 to "男", 2 to "女").forEach { (value, label) ->
                    val selected = uiState.gender == value
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) Primary else SurfaceColor,
                        modifier = Modifier.clickable { viewModel.onGenderChange(value) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            CommonTextField(
                value = uiState.bio,
                onValueChange = viewModel::onBioChange,
                label = "个人简介",
                placeholder = "介绍一下自己（选填）"
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}