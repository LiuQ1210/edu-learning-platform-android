package com.github.learningplatform.ui.article

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.surfaceWashBrush
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Surface as SurfaceColor
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/**
 * 文章编辑 / 发布（接口文档 7.3 新建、7.4 编辑）。
 *
 * 不设置封面：列表/详情页的封面由后端下发（列表接口本来就允许空），
 * 发布时不传 coverUrl，交给服务端决定默认表现。
 *
 * 已知限制：content 字段是 HTML。这里用纯文本编辑 + 段落换行转换为 <p>，
 * 满足「能发出文章」的最小闭环；接入富文本编辑器时只需替换 [BodyEditor]。
 */
@Composable
fun ArticleEditScreen(
    articleId: Long,
    onBack: () -> Unit,
    onPublished: () -> Unit,
    viewModel: ArticleEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(articleId) { viewModel.init(articleId) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(uiState.finished) {
        if (uiState.finished) onPublished()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BackTopBar(
                title = if (articleId > 0L) "编辑文章" else "发布文章",
                onBack = onBack,
                actions = {
                    TextButton(
                        onClick = { viewModel.submit(publish = 0) },
                        enabled = !uiState.submitting && uiState.title.isNotBlank() && uiState.categoryId != null
                    ) { Text("存草稿", color = TextSecondary) }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.submit(publish = 0) },
                        enabled = !uiState.submitting && uiState.title.isNotBlank() && uiState.categoryId != null,
                        modifier = Modifier.weight(1f)
                    ) { Text("保存草稿") }
                    Button(
                        onClick = { viewModel.submit(publish = 1) },
                        enabled = !uiState.submitting && uiState.canPublish,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text(if (uiState.submitting) "提交中..." else "发布")
                    }
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
                .padding(16.dp)
        ) {
            FieldLabel("标题")
            BasicTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceWashBrush())
                    .padding(14.dp),
                decorationBox = { inner ->
                    if (uiState.title.isEmpty()) {
                        Text("请输入标题", color = TextHint, style = MaterialTheme.typography.titleMedium)
                    }
                    inner()
                }
            )

            Spacer(Modifier.height(16.dp))
            FieldLabel("摘要（选填）")
            BasicTextField(
                value = uiState.summary,
                onValueChange = viewModel::onSummaryChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceWashBrush())
                    .padding(14.dp),
                decorationBox = { inner ->
                    if (uiState.summary.isEmpty()) {
                        Text("一句话概括本文内容", color = TextHint, style = MaterialTheme.typography.bodyMedium)
                    }
                    inner()
                }
            )

            Spacer(Modifier.height(16.dp))
            FieldLabel("分类")
            if (uiState.categories.isEmpty()) {
                Text("暂无分类", color = TextHint, style = MaterialTheme.typography.bodySmall)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.categories, key = { it.first }) { (id, name) ->
                        val selected = uiState.categoryId == id
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) Primary else SurfaceColor,
                            modifier = Modifier.clickable { viewModel.selectCategory(id) }
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            FieldLabel("标签（最多选 5 个）")
            if (uiState.tags.isEmpty()) {
                Text("暂无标签", color = TextHint, style = MaterialTheme.typography.bodySmall)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.tags, key = { it.tagId }) { tag ->
                        val selected = tag.tagId in uiState.selectedTagIds
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) Primary.copy(alpha = 0.12f) else SurfaceColor,
                            modifier = Modifier.clickable { viewModel.toggleTag(tag.tagId) }
                        ) {
                            Text(
                                text = "#${tag.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) Primary else TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            FieldLabel("正文")
            BodyEditor(
                value = uiState.content,
                onValueChange = viewModel::onContentChange
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${uiState.content.length} 字 · 空行分段",
                style = MaterialTheme.typography.labelSmall,
                color = TextHint
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun BodyEditor(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceWashBrush())
            .padding(14.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Row {
                    Icon(
                        NavIcons.Edit,
                        contentDescription = null,
                        tint = TextHint,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .height(16.dp)
                            .width(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "在这里写正文，空行表示分段",
                        color = TextHint,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            inner()
        }
    )
}
