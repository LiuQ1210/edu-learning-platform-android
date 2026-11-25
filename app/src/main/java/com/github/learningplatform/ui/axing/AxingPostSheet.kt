package com.github.learningplatform.ui.axing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val PostSuggestions = listOf(
    "这篇帖子讲了什么？",
    "帮我把要点列出来",
    "有没有相关的帖子？"
)

/**
 * 帖子内「问阿星」底部弹层。
 *
 * 回答来自 `/api/v1/agent/community/ask`，与助手主页共用同一套视觉。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxingPostSheet(
    articleTitle: String,
    onDismiss: () -> Unit,
    viewModel: AxingPostViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    fun ask(text: String) = viewModel.ask(articleTitle, text)

    LaunchedEffect(state.messages.size, state.thinking) {
        val last = state.messages.size + if (state.thinking) 1 else 0 - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .height(4.dp)
                    .width(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .navigationBarsPadding()
        ) {
            PostSheetHeader(articleTitle = articleTitle, onClose = {
                scope.launch {
                    sheetState.hide()
                    onDismiss()
                }
            })

            Box(modifier = Modifier.weight(1f)) {
                if (state.messages.isEmpty() && !state.thinking) {
                    PostSheetEmpty(onAsk = ::ask)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.messages.size) { index ->
                            when (val message = state.messages[index]) {
                                is AxingPostMessage.User -> PostUserBubble(message.text)
                                is AxingPostMessage.Answer -> PostAnswerCard(message.content)
                                is AxingPostMessage.Error -> PostErrorRow(
                                    text = message.text,
                                    onRetry = { viewModel.retry(articleTitle) }
                                )
                            }
                        }
                        if (state.thinking) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(15.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("阿星正在检索社区内容…", fontSize = 13.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            PostInputBar(
                input = state.input,
                onInputChange = viewModel::setInput,
                onSend = { ask(state.input) }
            )
        }
    }
}

@Composable
private fun PostSheetHeader(articleTitle: String, onClose: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AxingAvatar(size = 44.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "问阿星",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "关于《$articleTitle》",
                    fontSize = 12.sp,
                    color = TextHint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    NavIcons.Close,
                    contentDescription = "关闭",
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun PostSheetEmpty(onAsk: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "可以问我这篇帖子的内容、思路，或者有没有相关讨论。",
            fontSize = 15.sp,
            lineHeight = 23.sp,
            color = TextPrimary
        )
        Spacer(Modifier.height(16.dp))
        Text(text = "试试这样问", fontSize = 13.sp, color = TextHint)
        Spacer(Modifier.height(8.dp))
        PostSuggestions.forEach { text ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable { onAsk(text) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        AxingIcons.Sparkle,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = text,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        AxingIcons.ArrowUpRight,
                        contentDescription = null,
                        tint = TextHint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PostUserBubble(text: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Surface(shape = RoundedCornerShape(18.dp), color = Primary) {
            Text(
                text = text,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun PostAnswerCard(content: AxingPostAnswer) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content.paragraphs.forEachIndexed { index, paragraph ->
                if (index > 0) Spacer(Modifier.height(14.dp))
                Text(
                    text = paragraph,
                    fontSize = 15.sp,
                    lineHeight = 25.sp,
                    color = TextPrimary
                )
            }
            if (content.quotes.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(text = "引用帖子", fontSize = 12.sp, color = TextHint)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    content.quotes.forEach { quote ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Icon(
                                AxingIcons.PostAsk,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = quote.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = quote.excerpt,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostErrorRow(text: String, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = text,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = onRetry) { Text("重试") }
        }
    }
}

@Composable
private fun PostInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val canSend = input.isNotBlank()
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Divider, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (input.isBlank()) {
                        Text("就这篇帖子提问…", fontSize = 15.sp, color = TextHint)
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = onInputChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                        cursorBrush = SolidColor(Primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (canSend) Primary else MaterialTheme.colorScheme.outlineVariant)
                    .clickable(enabled = canSend, onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    AxingIcons.Send,
                    contentDescription = "发送",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
