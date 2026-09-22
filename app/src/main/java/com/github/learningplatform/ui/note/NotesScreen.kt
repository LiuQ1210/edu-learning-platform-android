package com.github.learningplatform.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.NoteDto
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.CommonTextField
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.MetaText
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.surfaceWashBrush
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Surface as SurfaceColor
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/**
 * 笔记列表（接口文档 10.1 - 10.5）。
 *
 * 两张展示形态：卡片网格（默认）+ 关键词搜索。
 * sourceType: 1-文章 2-视频/课程。
 */
@Composable
fun NotesScreen(
    onBack: () -> Unit,
    onOpenNote: (Long) -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 新建/保存/删除失败都会写 message，必须可见
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = true))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BackTopBar(title = "笔记本", onBack = onBack)

            CommonTextField(
                value = uiState.keyword,
                onValueChange = viewModel::onKeywordChange,
                label = "搜索笔记",
                placeholder = "输入标题或正文关键词",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(10.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(noteSourceTabs, key = { it.second }) { (value, label) ->
                    val active = uiState.sourceType == value
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(50),
                        color = if (active) Primary else SurfaceColor,
                        modifier = Modifier.clickable { viewModel.selectSource(value) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (active) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when {
                uiState.isLoading && uiState.notes.isEmpty() -> LoadingState()
                uiState.error != null && uiState.notes.isEmpty() ->
                    ErrorState(message = uiState.error.orEmpty(), onRetry = viewModel::refresh)
                uiState.notes.isEmpty() -> EmptyState(message = "还没有笔记，点右下角新建")
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.notes, key = { it.noteId }) { note ->
                        NoteCard(note = note, onClick = { onOpenNote(note.noteId) })
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private val noteSourceTabs = listOf(
    null to "全部",
    1 to "文章笔记",
    2 to "视频笔记"
)

@Composable
private fun NoteCard(note: NoteDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(surfaceWashBrush())
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(
            text = note.title.ifBlank { "无标题笔记" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = note.content,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (note.sourceType == 2) "视频" else "文章",
                style = MaterialTheme.typography.labelSmall,
                color = Primary
            )
            Spacer(Modifier.width(6.dp))
            MetaText(note.updateTime, modifier = Modifier.weight(1f))
        }
        if (note.videoTimestamp != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "进度 ${note.videoTimestamp / 60}:${(note.videoTimestamp % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.labelSmall,
                color = TextHint
            )
        }
    }
}


/**
 * 笔记详情 / 编辑（接口文档 10.3 PUT / 10.4 DELETE）。
 */
@Composable
fun NoteDetailScreen(
    noteId: Long,
    onBack: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) { viewModel.loadDetail(noteId) }

    LaunchedEffect(uiState.detailClosed) {
        if (uiState.detailClosed) onBack()
    }

    val note = uiState.currentNote

    Scaffold(
        topBar = {
            BackTopBar(
                title = "笔记详情",
                onBack = onBack,
                actions = {
                    TextButton(onClick = { pendingDelete = true }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        bottomBar = {
            if (note != null) {
                Button(
                    onClick = viewModel::saveCurrentNote,
                    enabled = !uiState.saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(if (uiState.saving) "保存中..." else "保存修改")
                }
            }
        }
    ) { padding ->
        if (note == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                LoadingState()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(pageGradientBrush(strong = true))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (note.sourceTitle.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(surfaceWashBrush())
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (note.sourceType == 2) NavIcons.Video else NavIcons.Community,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "来自：" + note.sourceTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            CommonTextField(
                value = uiState.editTitle,
                onValueChange = viewModel::onEditTitleChange,
                label = "标题",
                placeholder = "笔记标题"
            )
            Spacer(Modifier.height(14.dp))

            BasicTextField(
                value = uiState.editContent,
                onValueChange = viewModel::onEditContentChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceWashBrush())
                    .padding(14.dp),
                decorationBox = { inner ->
                    if (uiState.editContent.isEmpty()) {
                        Text("笔记内容", color = TextHint, style = MaterialTheme.typography.bodyMedium)
                    }
                    inner()
                }
            )
        }
    }

    if (pendingDelete) {
        AlertDialog(
            onDismissRequest = { pendingDelete = false },
            title = { Text("删除笔记") },
            text = { Text("删除后无法恢复，确定吗？") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = false
                    viewModel.deleteCurrentNote()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = false }) { Text("取消") }
            }
        )
    }
}
