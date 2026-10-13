package com.github.learningplatform.ui.mine

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import coil3.compose.AsyncImage
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.CommonTextField
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 预设头像色板（点击切换）
    val avatarColors = listOf(
        Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFFFFD93D),
        Color(0xFF6C5CE7), Color(0xFF00B894), Color(0xFFE17055)
    )
    var avatarIdx by remember { mutableIntStateOf(0) }

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
            Spacer(Modifier.height(16.dp))

            // 头像：点击从相册选图
            var avatarUri by remember { mutableStateOf<android.net.Uri?>(null) }
            val imagePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri -> avatarUri = uri }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(avatarColors[avatarIdx])
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "头像",
                            modifier = Modifier.size(80.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = uiState.nickname.take(1).ifEmpty { "我" },
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("点击头像从相册选择", fontSize = 12.sp, color = TextHint)
            }

            Spacer(Modifier.height(24.dp))
            CommonTextField(
                value = uiState.nickname,
                onValueChange = viewModel::onNicknameChange,
                label = "昵称",
                placeholder = "请输入昵称"
            )

            Spacer(Modifier.height(16.dp))
            CommonTextField(
                value = uiState.bio,
                onValueChange = viewModel::onBioChange,
                label = "个人简介",
                placeholder = "介绍一下自己（选填）"
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
                        color = if (selected) Primary else Color(0xFFFFF3E0),
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

            Spacer(Modifier.height(24.dp))
        }
    }
}
