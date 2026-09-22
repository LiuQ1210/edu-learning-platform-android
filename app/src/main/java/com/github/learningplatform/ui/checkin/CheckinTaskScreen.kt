package com.github.learningplatform.ui.checkin

import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.CheckinTaskDto
import com.github.learningplatform.data.remote.dto.CheckinType
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.surfaceWashBrush
import com.github.learningplatform.ui.theme.pageGradientBrush
import java.io.File

/**
 * 今日待签到任务（接口文档 9.1 / 9.2）。
 *
 * 按 checkinType 分发：1正常 / 2位置 / 3手势 / 4拍照。
 * 位置判定、手势比对、时间窗校验均由后端完成，客户端只采集参数。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinTaskScreen(
    onBack: () -> Unit,
    onOpenRecords: () -> Unit,
    viewModel: CheckinViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("签到") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(NavIcons.ArrowBack, contentDescription = "返回") }
                },
                actions = { TextButton(onClick = onOpenRecords) { Text("签到记录") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val m = Modifier.padding(padding).fillMaxSize().background(pageGradientBrush(strong = true))
        when {
            uiState.isLoading && uiState.tasks.isEmpty() -> LoadingState(m)
            uiState.error != null && uiState.tasks.isEmpty() ->
                ErrorState(message = uiState.error.orEmpty(), modifier = m, onRetry = viewModel::loadTasks)
            uiState.tasks.isEmpty() ->
                EmptyState(message = "今日暂无待签到任务", modifier = m, onRetry = viewModel::loadTasks)
            else -> LazyColumn(modifier = m) {
                items(uiState.tasks, key = { it.taskId }) { task ->
                    CheckinTaskItem(task) { viewModel.openPanel(task) }
                    HorizontalDivider()
                }
            }
        }
    }

    uiState.activeTask?.let { task ->
        CheckinPanel(
            task = task,
            submitting = uiState.submitting,
            uploadingPhoto = uiState.uploadingPhoto,
            photoUrl = uiState.uploadedPhotoUrl,
            localPhotoPath = uiState.localPhotoPath,
            onDismiss = viewModel::closePanel,
            onSubmitNormal = { viewModel.submit(task) },
            onSubmitLocation = { lon, lat -> viewModel.submit(task, longitude = lon, latitude = lat) },
            onSubmitGesture = { pattern -> viewModel.submit(task, gesturePattern = pattern) },
            onPickPhoto = viewModel::uploadPhoto,
            onSubmitPhoto = { viewModel.submitPhoto(task) },
            onClearPhoto = viewModel::clearPhoto
        )
    }
}

@Composable
private fun CheckinTaskItem(task: CheckinTaskDto, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = !task.signed, onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssistChip(onClick = {}, label = { Text(task.type.label) })
            Spacer(Modifier.width(8.dp))
            Text(task.taskName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (task.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(task.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            listOf(task.startTime, task.endTime).filter { it.isNotBlank() }.joinToString(" ~ "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (task.signed) {
            Spacer(Modifier.height(4.dp))
            Text("已签到", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckinPanel(
    task: CheckinTaskDto,
    submitting: Boolean,
    uploadingPhoto: Boolean,
    photoUrl: String?,
    localPhotoPath: String?,
    onDismiss: () -> Unit,
    onSubmitNormal: () -> Unit,
    onSubmitLocation: (Double, Double) -> Unit,
    onSubmitGesture: (String) -> Unit,
    onPickPhoto: (File) -> Unit,
    onSubmitPhoto: () -> Unit,
    onClearPhoto: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text(task.taskName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                task.type.label + " · " + task.startTime + " ~ " + task.endTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            when (task.type) {
                CheckinType.NORMAL -> NormalContent(submitting, onSubmitNormal)
                CheckinType.LOCATION -> LocationContent(task, submitting, onSubmitLocation)
                CheckinType.GESTURE -> GestureContent(submitting, onSubmitGesture)
                CheckinType.PHOTO -> PhotoContent(
                    uploading = uploadingPhoto,
                    submitting = submitting,
                    photoUrl = photoUrl,
                    localPhotoPath = localPhotoPath,
                    onPick = onPickPhoto,
                    onSubmit = onSubmitPhoto,
                    onClear = onClearPhoto
                )
            }
        }
    }
}

@Composable
private fun NormalContent(submitting: Boolean, onConfirm: () -> Unit) {
    Text("在签到时间范围内点击下方按钮即可完成签到。", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(20.dp))
    MainButton(if (submitting) "提交中..." else "立即签到", !submitting, onConfirm)
}

@Composable
private fun LocationContent(
    task: CheckinTaskDto,
    submitting: Boolean,
    onConfirm: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var location by remember { mutableStateOf<Location?>(null) }
    var locating by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun locate() {
        locating = true
        errorText = null
        LocationHelper.getCurrentLocation(
            context = context,
            onSuccess = { location = it; locating = false },
            onError = { errorText = it; locating = false }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) locate() else errorText = "未授予定位权限，无法完成位置签到"
    }

    Text("允许范围：${task.allowRadius} 米", style = MaterialTheme.typography.bodyMedium)

    val loc = location
    val lat = task.targetLatitude
    val lon = task.targetLongitude
    if (loc != null && lat != null && lon != null) {
        val distance = LocationHelper.distanceMeters(loc.latitude, loc.longitude, lat, lon)
        val inRange = distance <= task.allowRadius
        Spacer(Modifier.height(8.dp))
        Text(
            "当前位置距签到点约 ${distance.toInt()} 米" + if (inRange) "（在范围内）" else "（超出范围）",
            style = MaterialTheme.typography.bodyMedium,
            color = if (inRange) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }

    errorText?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }

    Spacer(Modifier.height(16.dp))
    OutlinedButton(
        onClick = {
            if (LocationHelper.hasPermission(context)) locate()
            else permissionLauncher.launch(LocationHelper.requiredPermissions)
        },
        enabled = !submitting && !locating,
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (locating) "定位中..." else if (loc == null) "获取当前位置" else "重新定位") }

    Spacer(Modifier.height(12.dp))
    MainButton(
        text = if (submitting) "提交中..." else "确认签到",
        enabled = loc != null && !submitting,
        onClick = { loc?.let { onConfirm(it.latitude, it.longitude) } }
    )
}

@Composable
private fun GestureContent(submitting: Boolean, onConfirm: (String) -> Unit) {
    var pattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var resetKey by remember { mutableIntStateOf(0) }

    Text("请绘制管理员预设的手势图案", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(8.dp))
    GestureLockView(enabled = !submitting, resetKey = resetKey, onPatternChanged = { pattern = it })
    Spacer(Modifier.height(8.dp))
    Text(
        if (pattern.isEmpty()) "按住并滑动绘制" else "已连接 ${pattern.size} 个点",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { resetKey++; pattern = emptyList() },
            enabled = !submitting,
            modifier = Modifier.weight(1f)
        ) { Text("重绘") }
        Button(
            onClick = { onConfirm(patternToString(pattern)) },
            enabled = pattern.size >= 4 && !submitting,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(48.dp)
        ) { Text(if (submitting) "提交中..." else "确认签到") }
    }
}

/**
 * 拍照签到（接口 v3.1 增补 15.1 上传 + 9.2 提交）。
 *
 * 两步：选照片 → 上传拿 url → 确认签到。上传中禁用按钮，避免重复上传。
 * 选图走系统相册（GetContent）：不需要相机权限，也不需要 FileProvider 配置，
 * 在模拟器上同样可用。
 */
@Composable
private fun PhotoContent(
    uploading: Boolean,
    submitting: Boolean,
    photoUrl: String?,
    localPhotoPath: String?,
    onPick: (File) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val file = uriToFile(context, uri)
        if (file == null) {
            android.widget.Toast.makeText(context, "无法读取所选照片", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            onPick(file)
        }
    }

    Text(
        "请上传一张现场照片作为签到凭证（支持 JPG/PNG，5 MB 以内）。",
        style = MaterialTheme.typography.bodyMedium
    )

    Spacer(Modifier.height(12.dp))

    // 预览：本地已选的照片
    if (localPhotoPath != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceWashBrush())
        ) {
            coil3.compose.AsyncImage(
                model = File(localPhotoPath),
                contentDescription = "签到照片预览",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    // 状态行
    Text(
        text = when {
            uploading -> "照片上传中..."
            photoUrl != null -> "照片已上传，可以提交签到"
            else -> "尚未选择照片"
        },
        style = MaterialTheme.typography.bodySmall,
        color = when {
            uploading -> MaterialTheme.colorScheme.primary
            photoUrl != null -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    )

    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { picker.launch("image/*") },
            enabled = !uploading && !submitting,
            modifier = Modifier.weight(1f)
        ) {
            Text(if (localPhotoPath == null) "选择照片" else "重新选择")
        }
        if (photoUrl != null) {
            OutlinedButton(
                onClick = onClear,
                enabled = !submitting,
                modifier = Modifier.weight(1f)
            ) { Text("清除") }
        }
    }

    Spacer(Modifier.height(12.dp))

    MainButton(
        text = if (submitting) "提交中..." else "确认签到",
        enabled = photoUrl != null && !submitting && !uploading,
        onClick = onSubmit
    )
}

/**
 * content:// -> 本地缓存文件。
 *
 * 系统返回的是 content Uri，OkHttp 无法直接读，必须先落到 cacheDir。
 */
private fun uriToFile(context: android.content.Context, uri: android.net.Uri): File? = runCatching {
    val target = File(context.cacheDir, "checkin_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    } ?: return null
    target.takeIf { it.length() > 0L }
}.getOrNull()

@Composable
private fun MainButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) { Text(text) }
}
