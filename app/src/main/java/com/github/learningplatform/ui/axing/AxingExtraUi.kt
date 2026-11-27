package com.github.learningplatform.ui.axing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.learningplatform.data.remote.dto.AgentMemoryDto
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.PrimaryContainer
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

// ---------------------------------------------------------------------------
// 执行型智能体：步骤时间线
// ---------------------------------------------------------------------------

/** 步骤时间线；[compact] 用于「思考中」气泡里的实时过程 */
@Composable
fun AxingStepsCard(
    steps: List<AxingStepUi>,
    note: String? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!compact) {
            Text(text = "执行过程", fontSize = 12.sp, color = TextHint)
            Spacer(Modifier.height(8.dp))
        }
        steps.forEachIndexed { index, step ->
            val isLast = index == steps.lastIndex
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val icon = when {
                        step.isError -> AxingIcons.StepError
                        step.kind == "observation" || step.status == "ok" -> AxingIcons.StepDone
                        else -> AxingIcons.StepPending
                    }
                    val tint = when {
                        step.isError -> MaterialTheme.colorScheme.error
                        step.kind == "observation" || step.status == "ok" -> Primary
                        else -> TextHint
                    }
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .width(1.dp)
                                .height(if (compact) 18.dp else 26.dp)
                                .background(Divider)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = if (isLast) 0.dp else 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = step.title,
                            fontSize = if (compact) 12.sp else 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (step.isError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                TextPrimary
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (step.elapsedText.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Text(text = step.elapsedText, fontSize = 11.sp, color = TextHint)
                        }
                    }
                    if (step.detail.isNotBlank()) {
                        Text(
                            text = step.detail,
                            fontSize = if (compact) 11.sp else 12.sp,
                            color = TextSecondary,
                            maxLines = if (compact) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }
        }
        note?.takeIf { it.isNotBlank() }?.let { text ->
            Spacer(Modifier.height(8.dp))
            Text(text = text, fontSize = 12.sp, color = TextHint)
        }
    }
}

// ---------------------------------------------------------------------------
// SQL 结果表
// ---------------------------------------------------------------------------

@Composable
fun AxingTableCard(table: AxingTable, modifier: Modifier = Modifier) {
    var showSql by remember(table.sql) { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Divider, RoundedCornerShape(10.dp))
                .horizontalScroll(rememberScrollState())
        ) {
            Column {
                Row(modifier = Modifier.background(PrimaryContainer)) {
                    table.headers.forEach { header ->
                        Text(
                            text = header,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .width(118.dp)
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                }
                table.rows.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.background(
                            if (rowIndex % 2 == 0) Color.Transparent
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        row.forEach { cell ->
                            Text(
                                text = cell,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .width(118.dp)
                                    .padding(horizontal = 8.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "共 ${table.rowCount} 行" +
                    if (table.rows.size < table.rowCount) " · 展示前 ${table.rows.size} 行" else "",
                fontSize = 12.sp,
                color = TextHint,
                modifier = Modifier.weight(1f)
            )
            if (table.sql.isNotBlank()) {
                TextButton(onClick = { showSql = !showSql }) {
                    Text(if (showSql) "收起 SQL" else "查看 SQL", fontSize = 12.sp)
                }
            }
        }
        if (showSql && table.sql.isNotBlank()) {
            Text(
                text = table.sql,
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp)
            )
        }
        if (table.error) {
            Spacer(Modifier.height(6.dp))
            Text(text = "查询没有通过只读视图校验", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
        }
    }
}

// ---------------------------------------------------------------------------
// 视频字幕
// ---------------------------------------------------------------------------

@Composable
fun AxingSubtitleCard(subtitle: AxingSubtitle, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    AxingIcons.Subtitle,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "视频字幕",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (subtitle.cached) {
                    Text(text = "已缓存", fontSize = 11.sp, color = TextHint)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle.title,
                fontSize = 13.sp,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val meta = buildString {
                append(subtitle.format.uppercase())
                if (subtitle.segments > 0) append(" · ${subtitle.segments} 句")
            }
            Text(text = meta, fontSize = 11.sp, color = TextHint)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SubtitleAction("下载 SRT") {
                    openUrl(context, subtitle.srtUrl.ifBlank { subtitle.vttUrl })
                }
                SubtitleAction("VTT 字幕") {
                    openUrl(context, subtitle.vttUrl.ifBlank { subtitle.srtUrl })
                }
                SubtitleAction("复制链接") {
                    copyText(context, subtitle.vttUrl.ifBlank { subtitle.srtUrl })
                }
            }
        }
    }
}

@Composable
private fun SubtitleAction(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun AxingTaskCard(info: AxingTaskInfo, modifier: Modifier = Modifier) {
    val (label, tint) = when {
        info.pending -> "进行中" to Primary
        info.failed -> "失败" to MaterialTheme.colorScheme.error
        info.canceled -> "已取消" to TextHint
        else -> "已完成" to Primary
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = info.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = tint.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = tint,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            val detail = info.detail.ifBlank {
                if (info.failed) info.errorMsg else ""
            }
            if (detail.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = detail,
                    fontSize = 12.sp,
                    color = if (info.failed) MaterialTheme.colorScheme.error else TextSecondary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 记忆候选：保存 / 忽略
// ---------------------------------------------------------------------------

@Composable
fun AxingMemoryOptions(
    items: List<AxingMemoryOption>,
    states: Map<String, String>,
    onSave: (AxingMemoryOption) -> Unit,
    onIgnore: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                AxingIcons.Memory,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "要记住这条吗？",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        Spacer(Modifier.height(6.dp))
        items.forEach { item ->
            Column(modifier = Modifier.padding(vertical = 3.dp)) {
                Text(text = item.content, fontSize = 13.sp, color = TextPrimary)
                if (item.reason.isNotBlank()) {
                    Text(text = item.reason, fontSize = 11.sp, color = TextHint)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (states[item.candidateId]) {
                        "saved" -> Text(text = "已保存为长期记忆", fontSize = 12.sp, color = Primary)
                        "ignored" -> Text(text = "已忽略", fontSize = 12.sp, color = TextHint)
                        else -> {
                            TextButton(
                                onClick = { onSave(item) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 0.dp,
                                    vertical = 0.dp
                                )
                            ) {
                                Text("保存", fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            TextButton(
                                onClick = { onIgnore(item.candidateId) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 0.dp,
                                    vertical = 0.dp
                                )
                            ) {
                                Text("忽略", fontSize = 13.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 长期记忆管理
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxingMemorySheet(
    state: AxingMemoryUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onEdit: (AgentMemoryDto) -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: (Long) -> Unit
) {
    if (!state.visible) return
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "长期记忆",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(NavIcons.Close, contentDescription = "关闭", tint = TextHint)
                }
            }
            Text(
                text = "记忆是用户级的，跨会话生效。单条最多 500 字、最多 50 条；" +
                    "请不要保存密码、身份证号等敏感信息。",
                fontSize = 12.sp,
                color = TextHint
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.draft,
                onValueChange = onDraftChange,
                label = { Text(if (state.editingId > 0) "编辑记忆" else "新增记忆") },
                placeholder = { Text("例如：我在学 Java，希望多举代码示例") },
                minLines = 2,
                maxLines = 4,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.draft.length}/500",
                    fontSize = 11.sp,
                    color = TextHint,
                    modifier = Modifier.weight(1f)
                )
                if (state.editingId > 0) {
                    TextButton(onClick = onCancelEdit, enabled = !state.busy) {
                        Text("取消编辑", fontSize = 13.sp)
                    }
                }
                Button(
                    onClick = onSave,
                    enabled = !state.busy && state.draft.isNotBlank()
                ) {
                    Text(if (state.busy) "保存中…" else if (state.editingId > 0) "更新" else "保存")
                }
            }
            state.error?.let { error ->
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = error,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRetry) { Text("重试") }
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Divider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已保存 ${state.items.size}/50",
                    fontSize = 12.sp,
                    color = TextHint,
                    modifier = Modifier.weight(1f)
                )
                if (state.loading) {
                    Text(text = "加载中…", fontSize = 12.sp, color = TextHint)
                }
            }
            when {
                state.items.isEmpty() && !state.loading -> Text(
                    text = "还没有长期记忆。聊天时阿星提议的信息，或在这里手动添加，都会跨会话生效。",
                    fontSize = 13.sp,
                    color = TextHint,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                else -> LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.items, key = { it.memoryId }) { memory ->
                        MemoryRow(
                            memory = memory,
                            onEdit = { onEdit(memory) },
                            onDelete = { onDelete(memory.memoryId) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MemoryRow(
    memory: AgentMemoryDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = memory.content, fontSize = 13.sp, color = TextPrimary)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = listOf(
                        if (memory.source == "manual") "手动添加" else "对话中保存",
                        memory.updateTime.ifBlank { memory.createTime }
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 11.sp,
                    color = TextHint
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    NavIcons.Edit,
                    contentDescription = "编辑记忆",
                    tint = TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    NavIcons.Delete,
                    contentDescription = "删除记忆",
                    tint = TextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 历史会话操作
// ---------------------------------------------------------------------------

@Composable
fun AxingSessionMenuDialog(
    item: AxingHistoryItem,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = item.title,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                SessionMenuRow("重命名会话", NavIcons.Edit, onRename)
                SessionMenuRow("删除会话", NavIcons.Delete, onDelete, danger = true)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SessionMenuRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (danger) MaterialTheme.colorScheme.error else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = if (danger) MaterialTheme.colorScheme.error else TextPrimary
        )
    }
}

@Composable
fun AxingRenameDialog(
    item: AxingHistoryItem,
    draft: String,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名会话", fontSize = 16.sp) },
        text = {
            Column {
                Text(
                    text = item.title,
                    fontSize = 12.sp,
                    color = TextHint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    singleLine = true,
                    label = { Text("会话标题") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = draft.isNotBlank()) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
fun AxingDeleteDialog(
    item: AxingHistoryItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除会话", fontSize = 16.sp) },
        text = {
            Text(
                text = "「${item.title}」及其消息会被删除，删除后不可恢复。",
                fontSize = 14.sp,
                color = TextPrimary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ---------------------------------------------------------------------------
// 小工具
// ---------------------------------------------------------------------------

private fun openUrl(context: Context, url: String) {
    if (url.isBlank()) {
        android.widget.Toast.makeText(context, "链接为空", android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        android.widget.Toast.makeText(context, "无法打开链接", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun copyText(context: Context, text: String) {
    if (text.isBlank()) return
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText("字幕链接", text))
    android.widget.Toast.makeText(context, "链接已复制", android.widget.Toast.LENGTH_SHORT).show()
}

/** 输入栏下方的能力选择：执行任务、数据查询、视频字幕都在这里 */
val AxingInputCapabilities = listOf(
    AxingCapability.SUMMARY,
    AxingCapability.QA,
    AxingCapability.VIDEO,
    AxingCapability.PPT,
    AxingCapability.ACT,
    AxingCapability.DATA,
    AxingCapability.SUBTITLE
)

