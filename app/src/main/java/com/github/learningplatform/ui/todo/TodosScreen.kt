package com.github.learningplatform.ui.todo

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.TodoDto
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.CommonTextField
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.MetaText
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Surface as SurfaceColor
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

private val todoTabs = listOf(
    null to "全部",
    0 to "未完成",
    1 to "已完成"
)

private val priorityOptions = listOf(1 to "高", 2 to "中", 3 to "低")

private fun priorityColor(priority: Int): Color = when (priority) {
    1 -> Color(0xFFE53935)
    2 -> Color(0xFFF57C00)
    else -> Color(0xFF43A047)
}

/**
 * 待办（接口文档 11.1 - 11.5）。
 *
 * 优先级 1高 2中 3低；状态 0未完成 1已完成（PATCH 单独接口切换）。
 */
@Composable
fun TodosScreen(
    onBack: () -> Unit,
    viewModel: TodosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<TodoDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 创建/删除/切换状态失败都会写 message，必须展示出来，否则用户只看到界面没反应
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
            BackTopBar(title = "待办", onBack = onBack)

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todoTabs, key = { it.second }) { (value, label) ->
                    val active = uiState.status == value
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(50),
                        color = if (active) Primary else SurfaceColor,
                        modifier = Modifier.clickable { viewModel.selectStatus(value) }
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
                uiState.isLoading && uiState.todos.isEmpty() -> LoadingState()
                uiState.error != null && uiState.todos.isEmpty() ->
                    ErrorState(message = uiState.error.orEmpty(), onRetry = viewModel::refresh)
                uiState.todos.isEmpty() -> EmptyState(message = "暂无待办，点右下角新建")
                else -> LazyColumn(
modifier = Modifier
    .fillMaxSize()
    .background(pageGradientBrush(strong = true)),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(uiState.todos, key = { it.todoId }) { todo ->
                        TodoRow(
                            todo = todo,
                            onToggle = { viewModel.toggleStatus(todo) },
                            onDelete = { pendingDelete = todo }
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Divider)
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = viewModel::openCreateDialog,
            containerColor = Primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(NavIcons.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("新建待办")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (uiState.showCreateDialog) {
        CreateTodoDialog(
            onDismiss = viewModel::dismissCreateDialog,
            onConfirm = { title, description, priority, deadline ->
                viewModel.create(title, description, priority, deadline)
            }
        )
    }

    pendingDelete?.let { todo ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除待办") },
            text = { Text("确定删除「${todo.title}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(todo.todoId)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun TodoRow(todo: TodoDto, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (todo.status == 1) Primary else Color.Transparent)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (todo.status == 1) {
                Icon(
                    NavIcons.Check,
                    contentDescription = "已完成",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Divider)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (todo.status == 1) TextHint else TextPrimary,
                    textDecoration = if (todo.status == 1) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = priorityOptions.firstOrNull { it.first == todo.priority }?.second ?: "中",
                    style = MaterialTheme.typography.labelSmall,
                    color = priorityColor(todo.priority)
                )
            }
            if (todo.description.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = todo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Row {
                if (todo.deadline.isNotBlank()) {
                    MetaText("截止 " + todo.deadline)
                }
                if (todo.sourceName.isNotBlank()) {
                    Spacer(Modifier.width(10.dp))
                    MetaText("来自 " + todo.sourceName)
                }
            }
        }

        Text(
            text = "删除",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onDelete)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CreateTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(2) }
    var deadline by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建待办") },
        text = {
            Column {
                CommonTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "标题",
                    placeholder = "要做什么"
                )
                Spacer(Modifier.height(12.dp))
                CommonTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "描述",
                    placeholder = "补充说明（选填）"
                )
                Spacer(Modifier.height(12.dp))
                CommonTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = "截止时间",
                    placeholder = "如 2026-03-01 18:00（选填）"
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "优先级",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priorityOptions.forEach { (value, label) ->
                        val active = priority == value
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) priorityColor(value) else SurfaceColor,
                            modifier = Modifier.clickable { priority = value }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (active) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(title, description, priority, deadline.ifBlank { null })
                },
                enabled = title.isNotBlank()
            ) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
