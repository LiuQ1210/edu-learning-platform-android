package com.github.learningplatform.ui.axing

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.PrimaryContainer
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

private val AxingPptBlue = Color(0xFF3B7BF6)
private val AxingPptBlueDark = Color(0xFF2E5FD0)
private val AxingInputBorder = Color(0xFFB9D2FB)

/**
 * 阿星 AI 学习助手（全屏）。
 *
 * 入口：首页右下角浮动头像、课程详情「问阿星」卡、播放页顶栏。
 * 回答全部来自 `/api/v1/agent`，学习数据来自学习记录接口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxingAssistantScreen(
    courseId: Long,
    courseTitle: String,
    videoId: Long = 0,
    onClose: () -> Unit,
    viewModel: AxingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(courseId, courseTitle, videoId) {
        viewModel.initCourse(courseId, courseTitle, videoId)
    }

    // 一次性提示（记忆保存 / 会话重命名等）
    LaunchedEffect(state.notice) {
        state.notice?.let { message ->
            toast(context, message)
            viewModel.consumeNotice()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.attachImage(it.toString()) } }

    // 新消息 / 思考态出现时滚到底部；流式每约 120 字跟一次，向上翻历史时 lastMessage 不变
    val lastMessage = state.messages.lastOrNull()
    val streamedBlocks = state.streamingText.length / 120
    LaunchedEffect(lastMessage, state.isThinking, streamedBlocks) {
        val last = state.messages.size + if (state.isThinking) 1 else 0 - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    BackHandler(enabled = state.showDrawer) { viewModel.closeDrawer() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AxingTopBar(
                subtitle = "学习不迷路，阿星来帮助",
                onOpenDrawer = viewModel::openDrawer,
                onClose = onClose
            )

            if (state.messages.isEmpty()) {
                AxingEmptyState(
                    modifier = Modifier.weight(1f),
                    onSuggestion = viewModel::useSuggestion
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (state.hasEarlier || state.loadingEarlier) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !state.loadingEarlier) {
                                        viewModel.loadEarlier()
                                    }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (state.loadingEarlier) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("正在加载更早的消息…", fontSize = 12.sp, color = TextHint)
                                } else {
                                    Text("加载更早的消息", fontSize = 12.sp, color = Primary)
                                }
                            }
                        }
                    }
                    itemsIndexed(state.messages) { index, message ->
                        when (message) {
                            is AxingMessage.User -> UserBubble(message)
                            is AxingMessage.Assistant -> {
                                AnswerSection(
                                    answer = message.answer,
                                    candidateStates = state.candidateStates,
                                    onOpenCoursePicker = viewModel::openCoursePicker,
                                    onRegenerate = viewModel::regenerate,
                                    onMakePpt = viewModel::makePpt,
                                    onOpenDeck = viewModel::openDeck,
                                    onSaveMemory = { option ->
                                        viewModel.saveMemoryCandidate(option.candidateId, option.content)
                                    },
                                    onIgnoreMemory = viewModel::ignoreMemoryCandidate
                                )
                            }
                            is AxingMessage.Error -> ErrorCard(
                                text = message.text,
                                onRetry = viewModel::regenerate
                            )
                        }
                    }
                    if (state.isThinking) {
                        item {
                            ThinkingBubble(
                                label = state.thinkingLabel,
                                streamingText = state.streamingText,
                                steps = state.liveSteps,
                                note = state.liveNote,
                                cancellable = state.activeTaskId > 0,
                                cancelling = state.cancelling,
                                onCancel = viewModel::cancelRunningTask
                            )
                        }
                    }
                }
            }

            AxingInputBar(
                input = state.input,
                capability = state.capability,
                course = state.course,
                imageUri = state.imageUri,
                onInputChange = viewModel::setInput,
                onSelectCapability = viewModel::selectCapability,
                onClearCapability = viewModel::clearCapability,
                onPickImage = { imagePicker.launch("image/*") },
                onClearImage = viewModel::clearImage,
                onSend = viewModel::send
            )
        }

        if (state.showCoursePicker) {
            AxingCoursePicker(
                selected = state.course,
                courses = state.courses,
                loading = state.coursesLoading,
                error = state.coursesError,
                onRetry = viewModel::retryCourses,
                onSelect = viewModel::selectCourse,
                onDismiss = viewModel::closeCoursePicker
            )
        }

        AxingHistoryDrawer(
            visible = state.showDrawer,
            activeSessionId = state.sessionId,
            items = state.sessions,
            loading = state.sessionsLoading,
            error = state.sessionsError,
            onRetry = viewModel::retrySessions,
            onDismiss = viewModel::closeDrawer,
            onNewChat = viewModel::newChat,
            onOpenHistory = viewModel::openHistory,
            onSessionMenu = viewModel::openSessionMenu,
            onOpenMemory = viewModel::openMemories,
            streamingReply = state.streamingReply,
            onToggleStreaming = viewModel::toggleStreamingReply,
            hasMoreSessions = state.sessions.size < state.sessionTotal,
            onLoadMoreSessions = viewModel::loadMoreSessions
        )

        AxingMemorySheet(
            state = state.memory,
            onDismiss = viewModel::closeMemories,
            onRetry = viewModel::retryMemories,
            onDraftChange = viewModel::setMemoryDraft,
            onSave = viewModel::saveMemoryDraft,
            onEdit = viewModel::editMemory,
            onCancelEdit = viewModel::cancelMemoryEdit,
            onDelete = viewModel::deleteMemory
        )

        state.sessionMenu?.let { item ->
            AxingSessionMenuDialog(
                item = item,
                onRename = { viewModel.startRename(item) },
                onDelete = { viewModel.startDelete(item) },
                onDismiss = viewModel::closeSessionMenu
            )
        }

        state.renameTarget?.let { item ->
            AxingRenameDialog(
                item = item,
                draft = state.renameDraft,
                onDraftChange = viewModel::setRenameDraft,
                onConfirm = viewModel::confirmRename,
                onDismiss = viewModel::cancelRename
            )
        }

        state.deleteTarget?.let { item ->
            AxingDeleteDialog(
                item = item,
                onConfirm = viewModel::confirmDelete,
                onDismiss = viewModel::cancelDelete
            )
        }
    }

    state.fullscreenDeck?.let { deck ->
        AxingDeckFullscreen(deck = deck, onClose = viewModel::closeDeck)
    }
}

// ---------------------------------------------------------------------------
// 顶栏
// ---------------------------------------------------------------------------

@Composable
private fun AxingTopBar(
    subtitle: String,
    onOpenDrawer: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton(icon = NavIcons.Menu, description = "历史对话", onClick = onOpenDrawer)
        Spacer(Modifier.width(10.dp))
        AxingAvatar(size = 44.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "阿星",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextHint
            )
        }
        CircleIconButton(icon = NavIcons.Close, description = "关闭", onClick = onClose)
    }
}

@Composable
fun AxingAvatar(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 48.dp) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        AsyncImage(
            model = AxingAvatarRes,
            contentDescription = "阿星",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape)
        )
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}

// ---------------------------------------------------------------------------
// 空状态
// ---------------------------------------------------------------------------

@Composable
private fun AxingEmptyState(
    modifier: Modifier = Modifier,
    onSuggestion: (AxingSuggestion) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text(
            text = "试试这样问",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(14.dp))
        AxingSuggestions.forEach { suggestion ->
            SuggestionCard(suggestion = suggestion, onClick = { onSuggestion(suggestion) })
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "点一下会填进输入框，阿星会自动判断用哪种能力。",
            fontSize = 13.sp,
            color = TextHint
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SuggestionCard(suggestion: AxingSuggestion, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(suggestion.tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    suggestion.icon,
                    contentDescription = null,
                    tint = suggestion.tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = suggestion.text,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                AxingIcons.PlusCircle,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 对话气泡与回答
// ---------------------------------------------------------------------------

@Composable
private fun UserBubble(message: AxingMessage.User) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Primary,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                message.imageUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "上传的图片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 160.dp, height = 110.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    if (message.text.isNotBlank()) Spacer(Modifier.height(8.dp))
                }
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerSection(
    answer: AxingAnswer,
    candidateStates: Map<String, String>,
    onOpenCoursePicker: () -> Unit,
    onRegenerate: () -> Unit,
    onMakePpt: () -> Unit,
    onOpenDeck: (AxingDeck) -> Unit,
    onSaveMemory: (AxingMemoryOption) -> Unit,
    onIgnoreMemory: (String) -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        AnswerMeta(answer = answer, onOpenCoursePicker = onOpenCoursePicker)
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                answer.heading?.let { heading ->
                    Text(
                        text = heading,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(10.dp))
                }
                answer.blocks.forEachIndexed { index, block ->
                    if (index > 0) Spacer(Modifier.height(10.dp))
                    when (block) {
                        is AxingBlock.Paragraph -> Text(
                            text = block.text,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            color = TextPrimary
                        )
                        is AxingBlock.Bullets -> BulletList(block.items)
                        is AxingBlock.Sources -> SourceList(block.items)
                        is AxingBlock.Quotes -> QuoteList(block.items)
                        is AxingBlock.Deck -> AxingDeckCard(
                            deck = block.deck,
                            onOpenDeck = { onOpenDeck(block.deck) },
                            onExport = { shareDeck(context, block.deck) }
                        )
                        is AxingBlock.Steps -> AxingStepsCard(
                            steps = block.items,
                            note = block.note
                        )
                        is AxingBlock.Table -> AxingTableCard(block.table)
                        is AxingBlock.SubtitleCard -> AxingSubtitleCard(block.subtitle)
                        is AxingBlock.TaskCard -> AxingTaskCard(block.info)
                        is AxingBlock.MemoryOptions -> AxingMemoryOptions(
                            items = block.items,
                            states = candidateStates,
                            onSave = onSaveMemory,
                            onIgnore = onIgnoreMemory
                        )
                    }
                }
                answer.caption?.let { caption ->
                    Spacer(Modifier.height(12.dp))
                    Text(text = caption, fontSize = 12.sp, color = TextHint)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnswerActionChip(label = "重新生成", icon = NavIcons.Refresh, onClick = onRegenerate)
            if (answer.capability != AxingCapability.PPT) {
                AnswerActionChip(label = "做成 PPT", icon = AxingIcons.Slideshow, onClick = onMakePpt)
            }
        }
    }
}

@Composable
private fun AnswerMeta(answer: AxingAnswer, onOpenCoursePicker: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "按「${answer.capability.label}」回答",
            fontSize = 13.sp,
            color = TextHint
        )
        answer.course?.let { course ->
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = PrimaryContainer,
                modifier = Modifier.clickable(onClick = onOpenCoursePicker)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = course.title,
                        fontSize = 13.sp,
                        color = Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 190.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        AxingIcons.ChevronDown,
                        contentDescription = "切换课程",
                        tint = Primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Row {
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Primary)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = item,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun SourceList(items: List<AxingSource>) {
    Column {
        Text(text = "来源", fontSize = 12.sp, color = TextHint)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        AxingIcons.Doc,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "${source.chapter} · ${source.title}",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    source.time?.let { time ->
                        Spacer(Modifier.width(8.dp))
                        Text(text = time, fontSize = 12.sp, color = TextHint)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteList(items: List<AxingQuote>) {
    Column {
        Text(text = "引用帖子", fontSize = 12.sp, color = TextHint)
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { quote ->
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
                        Text(text = quote.excerpt, fontSize = 13.sp, lineHeight = 19.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerActionChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(text = label, fontSize = 13.sp, color = TextPrimary)
        }
    }
}

@Composable
private fun ThinkingBubble(
    label: String,
    streamingText: String = "",
    steps: List<AxingStepUi> = emptyList(),
    note: String? = null,
    cancellable: Boolean = false,
    cancelling: Boolean = false,
    onCancel: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                if (cancellable) {
                    TextButton(onClick = onCancel, enabled = !cancelling) {
                        Text(
                            text = if (cancelling) "停止中…" else "停止",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            if (streamingText.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = streamingText,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = TextPrimary
                )
            }
            if (steps.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                AxingStepsCard(steps = steps, note = note, compact = true)
            } else if (!note.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(text = note, fontSize = 12.sp, color = TextHint)
            }
        }
    }
}

@Composable
private fun ErrorCard(text: String, onRetry: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = text,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnswerActionChip(label = "重试", icon = NavIcons.Refresh, onClick = onRetry)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PPT 卡片 + 全屏演示
// ---------------------------------------------------------------------------

@Composable
private fun AxingDeckCard(deck: AxingDeck, onOpenDeck: () -> Unit, onExport: () -> Unit) {
    Column {
        Text(
            text = deck.title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AxingIcons.Slideshow, contentDescription = null, tint = Primary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("已生成 ${deck.slides.size} 页幻灯片", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.weight(1f))
            Text("1 / ${deck.slides.size}", fontSize = 13.sp, color = TextHint)
        }
        Spacer(Modifier.height(10.dp))
        AxingSlideSurface(
            slide = deck.slides.first(),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clickable(onClick = onOpenDeck)
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(deck.slides.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(4.dp)
                        .width(if (index == 0) 18.dp else 6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index == 0) Primary else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Primary,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clickable(onClick = onOpenDeck)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(NavIcons.Play, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("全屏演示", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Primary.copy(alpha = 0.12f),
                modifier = Modifier
                    .width(112.dp)
                    .height(46.dp)
                    .clickable(onClick = onExport)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AxingIcons.Export, contentDescription = null, tint = Primary, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("导出", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                }
            }
        }
    }
}

@Composable
private fun AxingSlideSurface(slide: AxingSlide, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(AxingPptBlue, AxingPptBlueDark)))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 44.dp, y = (-46).dp)
                .size(170.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 52.dp, y = 54.dp)
                .size(130.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    AxingIcons.Sparkle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(slide.eyebrow, fontSize = 12.sp, color = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = slide.title,
                fontSize = if (slide.bullets.isEmpty()) 25.sp else 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (slide.bullets.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                slide.bullets.take(4).forEach { bullet ->
                    Row(
                        modifier = Modifier.padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = bullet,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (slide.subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = slide.subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.weight(0.6f))
            Text(slide.footer, fontSize = 11.sp, color = Color.White.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun AxingDeckFullscreen(deck: AxingDeck, onClose: () -> Unit) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val pagerState = rememberPagerState(pageCount = { deck.slides.size })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0D12))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deck.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${pagerState.currentPage + 1} / ${deck.slides.size}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
                DarkCircleButton(AxingIcons.Export) { shareDeck(context, deck) }
                Spacer(Modifier.width(10.dp))
                DarkCircleButton(NavIcons.Close, onClose)
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // 内边距放在 Pager 自身而非 contentPadding：用 contentPadding 时会
                    // 在右侧留白里透出下一张幻灯片的边缘，和设计稿的整页切换不一致
                    .padding(horizontal = 20.dp)
            ) { page ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AxingSlideSurface(
                        slide = deck.slides[page],
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(deck.slides.size) { index ->
                    val active = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(5.dp)
                            .width(if (active) 20.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (active) Color.White else Color.White.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}

@Composable
private fun DarkCircleButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
    }
}

private fun shareDeck(context: Context, deck: AxingDeck) {
    // 后端已经生成 pptx：直接打开下载地址，比分享文字更贴近「导出」
    if (deck.fileUrl.isNotBlank()) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(deck.fileUrl)))
        }.onFailure { toast(context, "无法打开下载链接") }
        return
    }
    val text = buildString {
        appendLine(deck.title)
        deck.slides.forEachIndexed { index, slide ->
            appendLine()
            appendLine("${index + 1}. ${slide.title}")
            slide.bullets.forEach { appendLine("· $it") }
        }
    }
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "导出课程汇报"))
    }
}

// ---------------------------------------------------------------------------
// 输入栏
// ---------------------------------------------------------------------------

@Composable
private fun AxingInputBar(
    input: String,
    capability: AxingCapability?,
    course: AxingCourse?,
    imageUri: String?,
    onInputChange: (String) -> Unit,
    onSelectCapability: (AxingCapability) -> Unit,
    onClearCapability: () -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onSend: () -> Unit
) {
    val placeholder = when {
        capability != null -> "可以补充要求，也可以直接发送"
        course != null -> "就《${course.title}》提问…"
        else -> "问课程、查数据、做 PPT..."
    }
    val canSend = input.isNotBlank() || imageUri != null || capability != null

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = if (canSend) AxingInputBorder else Divider,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (imageUri != null) {
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "待发送图片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3A3A44))
                                .clickable(onClick = onClearImage),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                NavIcons.Close,
                                contentDescription = "移除图片",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary, lineHeight = 22.sp),
                    cursorBrush = SolidColor(Primary),
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    decorationBox = { inner ->
                        Box {
                            if (input.isBlank()) {
                                Text(text = placeholder, fontSize = 15.sp, color = TextHint)
                            }
                            inner()
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = onPickImage),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            NavIcons.Add,
                            contentDescription = "添加图片",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(AxingInputCapabilities) { cap ->
                            CapabilityChip(
                                capability = cap,
                                selected = capability == cap,
                                onClick = { onSelectCapability(cap) },
                                onClear = onClearCapability
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (canSend) Primary else MaterialTheme.colorScheme.outlineVariant)
                            .clickable(enabled = canSend, onClick = onSend),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            AxingIcons.Send,
                            contentDescription = "发送",
                            tint = if (canSend) Color.White else Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityChip(
    capability: AxingCapability,
    selected: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    val (icon, tint) = capabilityIcon(capability)
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Primary else Divider
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(
                start = 10.dp,
                end = if (selected) 6.dp else 12.dp,
                top = 7.dp,
                bottom = 7.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else tint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = capability.label,
                fontSize = 12.sp,
                color = if (selected) Color.White else TextSecondary
            )
            if (selected) {
                Spacer(Modifier.width(2.dp))
                Icon(
                    AxingIcons.ChevronDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClear
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        NavIcons.Close,
                        contentDescription = "取消能力",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 课程选择弹层
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AxingCoursePicker(
    selected: AxingCourse?,
    courses: List<AxingCourse>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onSelect: (AxingCourse) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "选一门课程",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(text = "选好后只用于这条消息", fontSize = 14.sp, color = TextHint)
                }
                CircleIconButton(NavIcons.Close, "关闭", onDismiss)
            }
            Spacer(Modifier.height(18.dp))
            when {
                loading && courses.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("正在加载课程…", fontSize = 14.sp, color = TextSecondary)
                }
                error != null && courses.isEmpty() -> Column {
                    Text(text = error, fontSize = 14.sp, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onRetry) { Text("重试") }
                }
                courses.isEmpty() -> Text(
                    text = "暂无可选课程",
                    fontSize = 14.sp,
                    color = TextHint
                )
                else -> LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(courses) { course ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(course) }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = course.title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (course.id == selected?.id) Primary else TextPrimary
                            )
                            if (course.subtitle.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(text = course.subtitle, fontSize = 13.sp, color = TextHint)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 历史对话抽屉
// ---------------------------------------------------------------------------

@Composable
private fun AxingHistoryDrawer(
    visible: Boolean,
    activeSessionId: Long,
    items: List<AxingHistoryItem>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onNewChat: () -> Unit,
    onOpenHistory: (AxingHistoryItem) -> Unit,
    onSessionMenu: (AxingHistoryItem) -> Unit,
    onOpenMemory: () -> Unit,
    streamingReply: Boolean,
    onToggleStreaming: () -> Unit,
    hasMoreSessions: Boolean,
    onLoadMoreSessions: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it },
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AxingAvatar(size = 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "阿星",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        CircleIconButton(NavIcons.Close, "关闭抽屉", onDismiss)
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryContainer)
                            .clickable(onClick = onNewChat),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(14.dp))
                        Icon(
                            AxingIcons.NewChat,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "新对话",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(text = "历史对话", fontSize = 12.sp, color = TextHint)
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when {
                            loading && items.isEmpty() -> Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("正在加载历史…", fontSize = 13.sp, color = TextSecondary)
                            }
                            error != null && items.isEmpty() -> Column(
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(text = error, fontSize = 13.sp, color = TextSecondary)
                                TextButton(onClick = onRetry) { Text("重试") }
                            }
                            items.isEmpty() -> Text(
                                text = "还没有历史对话",
                                fontSize = 13.sp,
                                color = TextHint,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                            else -> items.forEach { item ->
                                val selected = item.sessionId == activeSessionId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) PrimaryContainer else Color.Transparent)
                                        .clickable { onOpenHistory(item) }
                                        .padding(start = 8.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 16.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (selected) Primary else TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(3.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                sceneIcon(item.scene),
                                                contentDescription = null,
                                                tint = sceneTint(item.scene),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = item.meta,
                                                fontSize = 12.sp,
                                                color = if (selected) {
                                                    Primary.copy(alpha = 0.7f)
                                                } else {
                                                    TextHint
                                                }
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onSessionMenu(item) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            NavIcons.MoreVert,
                                            contentDescription = "会话操作",
                                            tint = if (selected) Primary else TextHint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                        if (hasMoreSessions) {
                            TextButton(
                                onClick = onLoadMoreSessions,
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (loading) "正在加载更多…" else "加载更多历史",
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = Divider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            AxingIcons.Act,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "流式回答", fontSize = 15.sp, color = TextPrimary)
                            Text(
                                text = "逐字返回；关闭时提供记忆候选",
                                fontSize = 11.sp,
                                color = TextHint
                            )
                        }
                        Switch(
                            checked = streamingReply,
                            onCheckedChange = { onToggleStreaming() }
                        )
                    }
                    HorizontalDivider(color = Divider)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenMemory)
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            AxingIcons.Memory,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "记忆管理",
                            fontSize = 15.sp,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            AxingIcons.ChevronRight,
                            contentDescription = null,
                            tint = TextHint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun toast(context: Context, message: String) {
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
}
