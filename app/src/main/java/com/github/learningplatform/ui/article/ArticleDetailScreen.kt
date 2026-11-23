package com.github.learningplatform.ui.article

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.ArticleDetailDto
import com.github.learningplatform.data.remote.dto.CommentDto
import com.github.learningplatform.data.remote.dto.HotReplyDto
import com.github.learningplatform.data.remote.dto.PostStatus
import com.github.learningplatform.ui.common.Avatar
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.WriteNoteDialog
import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.MetaText
import com.github.learningplatform.ui.common.SectionHeader
import com.github.learningplatform.ui.common.TagChip
import com.github.learningplatform.ui.common.formatCount
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.HighlightWash
import com.github.learningplatform.ui.theme.surfaceWashBrush
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Surface as SurfaceColor
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/**
 * 文章详情（接口文档 7.2 详情 / 8.1 评论 / 8.9 收藏 / 8.10 点赞 / 8.3 发评论）。
 *
 * 正文是后端返回的富文本 HTML（ArticleDetailDto.content），用 WebView 渲染：
 *  - 注入 CSS 适配深色模式与移动端字号；
 *  - 拦截外链，交给系统浏览器，避免 WebView 内跳走导致排版丢失。
 */
@Composable
fun ArticleDetailScreen(
    articleId: Long,
    onBack: () -> Unit,
    onOpenAuthor: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onOpenTag: (Long) -> Unit,
    viewModel: ArticleDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // 从通知点进来时高亮指定评论
    val highlightCommentId = remember { CommentHighlightBus.highlightCommentId.also { CommentHighlightBus.highlightCommentId = null } }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // 滚过首屏后才显示回到顶部按钮
    val showBackToTop by derivedStateOf { listState.firstVisibleItemIndex > 0 }
    var commentInput by remember { mutableStateOf("") }
    // 正文超出详情页可视区 2/3 时折叠到 2/3，点击「查看全部内容」展开全文
    var contentExpanded by remember { mutableStateOf(false) }
    // 正文高度测出来之前先不铺评论区：否则正文从 48dp 撑到真实高度时，
    // 下方评论会当着用户的面往下挪一截，看起来像「评论在慢慢下移」
    var contentReady by remember(articleId) { mutableStateOf(false) }
    // 回复目标：parentId 决定「挂到哪条评论下」，name 只用于输入框上方的提示
    var replyToCommentId by remember { mutableStateOf<Long?>(null) }
    var replyToName by remember { mutableStateOf<String?>(null) }

    // 只读态给一句轻提示（评论/点赞/收藏后端都要求已发布）
    val context = androidx.compose.ui.platform.LocalContext.current
    val toast: (String) -> Unit = { msg ->
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(articleId) { viewModel.load(articleId) }

    // 帖子积分：正文加载后按「页面处于前台的秒数」计时，每满一个上报周期就把增量交给服务端，
    // 由服务端判断是否满 30 秒、是否已达每日上限。切后台不计时，避免把后台停留算成阅读。
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val detailLoaded = uiState.detail != null
    LaunchedEffect(articleId, lifecycleOwner, detailLoaded) {
        if (!detailLoaded) return@LaunchedEffect
        while (true) {
            delay(1_000)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(
                    androidx.lifecycle.Lifecycle.State.RESUMED
                )
            ) {
                viewModel.onForegroundSecond()
            }
        }
    }
    // 离开页面时把最后不足一个上报周期的秒数也发出去，否则每次不足 20 秒的阅读都会被丢弃
    DisposableEffect(articleId) {
        onDispose { viewModel.flushReading(force = true) }
    }

    // 从通知点进来时自动滚动到高亮评论；目标若是二级回复，则滚动到其父一级评论
    LaunchedEffect(highlightCommentId, uiState.comments.size) {
        val target = highlightCommentId ?: return@LaunchedEffect
        val rootIdx = uiState.comments.indexOfFirst { it.commentId == target }
        val parentIdx = if (rootIdx >= 0) rootIdx else
            uiState.comments.indexOfFirst { c -> c.hotReplies.any { r -> r.commentId == target } }
        if (parentIdx >= 0) {
            // 前面有 3 个 item：Header、正文、评论标题
            listState.animateScrollToItem(3 + parentIdx)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }
    // 本人查非已发布文章：评论/点赞/收藏接口都要求 status='0'，前端统一按只读处理
    val published = uiState.mineStatus == PostStatus.PUBLISHED

    Scaffold(
        // 奶油渐变铺在 Scaffold 容器上，正文与评论区共用同一底色
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.background(pageGradientBrush(strong = true)),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            BackTopBar(
                title = "文章详情",
                onBack = onBack,
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(NavIcons.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("写笔记") },
                                onClick = {
                                    menuExpanded = false
                                    // 笔记接口同样只认已发布内容，审核中直接拦下
                                    if (published) viewModel.showNoteDialog()
                                    else toast("文章审核通过后才能记笔记")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("举报") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.showReport()
                                }
                            )
                            // 发布后需走审核，后端没有 post/edit，本人也只能看不能改，
                            // 所以右上角菜单不再提供「编辑」
                        }
                    }
                }
            )
        },
        bottomBar = {
            ArticleBottomBar(
                likeCount = uiState.detail?.likeCount ?: 0,
                commentCount = uiState.detail?.commentCount ?: 0,
                onOpenComments = {
                    scope.launch { listState.animateScrollToItem(2) }
                },
                isLiked = uiState.isLiked,
                isFavorited = uiState.isFavorited,
                commentInput = commentInput,
                onCommentChange = { commentInput = it },
                replyToName = replyToName,
                onCancelReply = {
                    replyToName = null
                    replyToCommentId = null
                },
                onSendComment = {
                    if (commentInput.isNotBlank()) {
                        // 回复时带 parentId，后端才会把它挂到该评论的 children 下
                        viewModel.postComment(commentInput, replyToCommentId ?: 0L)
                        commentInput = ""
                        replyToName = null
                        replyToCommentId = null
                    }
                },
                onToggleLike = if (published) viewModel::toggleLike else ({ toast("文章审核通过后才能点赞") }),
                onToggleFavorite = if (published) viewModel::toggleFavorite else ({ toast("文章审核通过后才能收藏") }),
                onShare = viewModel::share,
                commentEnabled = published
            )
        }
    ) { padding ->
        val m = Modifier
            .padding(padding)
            .fillMaxSize()

        when {
            uiState.isLoading -> LoadingState(m)
            uiState.error != null -> ErrorState(
                message = uiState.error.orEmpty(),
                modifier = m,
                onRetry = { viewModel.load(articleId) }
            )
            uiState.detail == null -> EmptyState(message = "文章不存在", modifier = m)
            else -> {
                val detail = uiState.detail!!
                // 正文/评论所在的可视区高度（已由 Scaffold 扣掉顶栏与底部输入栏），折叠阈值按它取 2/3
                BoxWithConstraints(modifier = m) {
                val viewportDp = maxHeight.value.toInt()
                // 兜底：万一 WebView 测不出高度（图片裂图等），也不要让评论区一直不出现
                LaunchedEffect(detail) {
                    kotlinx.coroutines.delay(1200)
                    contentReady = true
                }
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(pageGradientBrush(strong = true)), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item {
                        ArticleHeader(
                            detail = detail,
                            onOpenAuthor = { onOpenAuthor(detail.authorId) },
                            onOpenTag = onOpenTag
                        )
                    }

                    // 本人文章不在「已发布」时，详情页顶部说明当前状态：作者能看正文，别人看不到
                    if (uiState.isMine && uiState.mineStatus != PostStatus.PUBLISHED) {
                        item { ArticleStatusNotice(uiState.mineStatus) }
                    }

                    item {
                        RichContent(
                            html = detail.content,
                            expanded = contentExpanded,
                            onExpand = { contentExpanded = true },
                            onCollapse = { contentExpanded = false },
                            viewportDp = viewportDp,
                            onReady = { contentReady = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // 正文高度确定后再一起铺评论区，避免评论区被正文撑高一步步推下去
                    // 非已发布（审核中/草稿/驳回/下架）不给评论区：后端评论接口只认已发布内容
                    if (contentReady && published) {
                    item { SectionHeader(title = "评论 (${detail.commentCount})") }

                    if (uiState.comments.isEmpty() && !uiState.commentsLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("还没有评论", color = TextHint, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // 首次拉取评论期间：标题下给一个小尺寸居中 loading，避免留一大块空白
                    if (uiState.comments.isEmpty() && uiState.commentsLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    items(uiState.comments, key = { it.commentId }) { comment ->
                        CommentItem(
                            comment = comment,
                            highlighted = comment.commentId == highlightCommentId,
                            highlightReplyId = highlightCommentId,
                            isMine = comment.userId == uiState.myUserId,
                            myUserId = uiState.myUserId,
                            onDelete = { viewModel.deleteComment(comment.commentId) },
                            onDeleteReply = { viewModel.deleteComment(it) },
                            onReply = {
                                replyToCommentId = comment.commentId
                                replyToName = comment.userName
                            },
                            onReplyReply = { reply ->
                                replyToCommentId = reply.commentId
                                replyToName = reply.userName
                            }
                        )
                    }

                    if (uiState.commentsHasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.commentsLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "加载更多评论",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(surfaceWashBrush())
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .clickable { viewModel.loadMoreComments() }
                                    )
                                }
                            }
                        }
                    }
                    } else if (contentReady) {
                        // 待审核/草稿等：不请求评论，也不留一块空评论区
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "文章审核通过后，这里才会收到评论",
                                    color = TextHint,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                if (showBackToTop) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 72.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { scope.launch { listState.animateScrollToItem(0) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            NavIcons.ArrowUp,
                            contentDescription = "回到顶部",
                            modifier = Modifier.size(20.dp),
                            tint = Primary
                        )
                    }
                }
                }
            }
        }
    }

    if (uiState.showReportDialog) {
        ReportDialog(
            onDismiss = viewModel::dismissReport,
            onSubmit = viewModel::submitReport
        )
    }

    if (uiState.showNoteDialog) {
        WriteNoteDialog(
            sourceType = Constants.TARGET_ARTICLE,
            sourceTitle = uiState.detail?.title.orEmpty(),
            saving = uiState.noteSaving,
            onDismiss = viewModel::dismissNoteDialog,
            onSave = viewModel::createNote
        )
    }
}

@Composable
private fun ArticleHeader(
    detail: ArticleDetailDto,
    onOpenAuthor: () -> Unit,
    onOpenTag: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = detail.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // 作者可点：进入 3.3 他人博客主页
            modifier = Modifier.clickable(enabled = detail.authorId > 0L, onClick = onOpenAuthor)
        ) {
            Avatar(url = detail.authorAvatar, name = detail.authorName, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = detail.authorName.ifBlank { "匿名作者" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                MetaText(detail.publishTime)
            }
            if (detail.categoryName.isNotBlank()) {
                TagChip(detail.categoryName)
            }
        }

        if (detail.tags.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                detail.tags.take(8).forEach { tag ->
                    Box(modifier = Modifier.clickable { onOpenTag(tag.tagId) }) {
                        TagChip("#${tag.name}")
                    }
                }
            }
        }

    }
}

/**
 * 富文本正文。
 *
 * 用 WebView 而非 Compose 手写解析：v3 的 content 是完整 HTML（含 img/table/code），
 * 手写 HTML -> AnnotatedString 会丢失表格与嵌套列表。
 *
 * 两个坑（都已处理）：
 *  1. LazyColumn 的 item 高度是无界的，直接塞 WebView 会渲染成 ~0 高（正文看不见）。
 *     这里测量回调把真实内容高度写回 Compose，用它钉住 item 高度。
 *  2. loadData 不能放在 update 里：update 每次重组都会跑（点赞、评论数变化等），
 *     会反复重载 HTML（闪烁 + 内部滚动位置丢失）。只在 document 变化时重新加载。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RichContent(
    html: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    viewportDp: Int,
    onReady: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (html.isBlank()) {
        Text(
            text = "暂无正文",
            style = MaterialTheme.typography.bodyMedium,
            color = TextHint,
            modifier = modifier
        )
        return
    }

    val dark = com.github.learningplatform.ui.theme.LocalAppDarkTheme.current
    val textColor = if (dark) "#E8EAF0" else "#1A1A1A"
    val borderColor = if (dark) "#353B4D" else "#DDD"
    val preBg = if (dark) "#28304A" else "#F5F5F5"
    val quoteColor = if (dark) "#9AA3B5" else "#666666"
    val linkColor = if (dark) "#B8CCFF" else "#1A73E8"

    // 正文是发布页写入的 markdown 源文（插图存成 ![](url)），先做最小转换再注入 WebView，
    // 否则图片语法会当成文字整段铺成 URL
    val rendered = remember(html) { renderArticleMarkdown(html) }

    val document = remember(rendered, dark) {
        """
        <!DOCTYPE html>
        <html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
          /* 正文高度由外层 Compose 控制：折叠时只露出截断部分，
             所以页面自身不允许滚动，否则手势能在 WebView 内部滚出被裁掉的正文 */
          html, body { overflow:hidden; }
          body { margin:0; padding:0; background:transparent; color:$textColor;
                 font-size:16px; line-height:1.75; word-break:break-word;
                 font-family:-apple-system, "PingFang SC", "Microsoft YaHei", sans-serif; }
          /* 正文插图单独成行，避免和上下文字挤在一起 */
          img, video { display:block; max-width:100%; height:auto; border-radius:8px; margin:8px 0; }
          h1, h2, h3, h4 { margin:12px 0 6px; font-weight:700; line-height:1.4; color:$textColor; }
          h1 { font-size:20px; } h2 { font-size:18px; } h3 { font-size:17px; } h4 { font-size:16px; }
          table { width:100%; border-collapse:collapse; }
          td, th { border:1px solid $borderColor; padding:6px; }
          pre { background:$preBg; color:$textColor; padding:10px; border-radius:8px; overflow-x:auto; }
          code { font-family:Consolas, monospace; }
          blockquote { margin:0; padding-left:12px; border-left:3px solid $linkColor; color:$quoteColor; }
          a { color:$linkColor; text-decoration:none; }
        </style></head>
        <body>$rendered</body></html>
        """.trimIndent()
    }

    // 初始给一个可见的最小高度，测量完成后按真实内容高度替换；
    // 初始值压到一行左右，避免正文还没测出来就撑出一大块空白
    var contentHeightDp by remember { mutableIntStateOf(48) }
    // 首次测到真实高度前先不显示正文：否则 48dp 的裁切态会先闪一下再撑开
    var measured by remember { mutableStateOf(false) }

    // 超长正文折叠：正文连同下方的「查看全部内容」共占「详情页可视区」的 2/3，
    // 超出部分截断，点按钮再展开全文；预留 48dp 给按钮，避免按钮被底部输入栏挤掉
    val maxContentDp = ((viewportDp * 2f / 3f).toInt() - 48).coerceAtLeast(160)
    // contentHeightDp 里含 16dp 底部安全余量，判断是否超长时先扣掉，
    // 避免余量把刚好处在 2/3 的正文顶出阈值，冒出一个多余的「查看全部内容」
    val contentOnlyDp = (contentHeightDp - 16).coerceAtLeast(0)
    val overflow = contentOnlyDp > maxContentDp
    val displayedDp = if (expanded) contentHeightDp else minOf(contentHeightDp, maxContentDp)

    // 折叠态底部渐隐色：取正文区域（页面渐变中段）的底色，深色单独给一支
    val fadeColor = if (dark) Color(0xFF191F2C) else Color(0xFFFBF2F1)

    Column(modifier = modifier) {
    // 正文与底部渐隐叠在同一层：渐隐只盖住被截断的最后一段，让裁切不生硬
    Box(modifier = Modifier.fillMaxWidth()) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(displayedDp.dp)
            // WebView 是原生视图，内容再高也会照旧绘制，必须显式裁到容器高度内，
            // 否则带图的长正文会溢出盖住下方的评论标题
            .clipToBounds()
            .clip(RoundedCornerShape(0.dp))
            .then(if (measured) Modifier else Modifier.alpha(0f)),
        factory = { context ->
            WebView(context).apply {
                // 只开 JS 用于内容高度回传，页面本身不执行任何脚本
                settings.javaScriptEnabled = true
                settings.defaultTextEncodingName = "UTF-8"
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    settings.forceDark = android.webkit.WebSettings.FORCE_DARK_AUTO
                }
                // 正文背景透明，跟随页面渐变底色，不再贴一张白卡
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                // 折叠时正文被裁断，禁止 WebView 自己吞手势滚出按钮下面的内容，
                // 滚动一律交给外层 LazyColumn
                isScrollContainer = false
                overScrollMode = android.view.View.OVER_SCROLL_NEVER

                fun measureHeight() {
                    // 用 body 的真实高度：WebView 的 contentHeight 在内容比视口短时会回落到视口高度，
                    // 拿它当正文高度会让容器自己把自己撑高（一行文字会被一步步推到 144dp），
                    // 评论区跟着往下挪，就是用户看到的「评论慢慢下移」
                    evaluateJavascript(
                        "(function(){var b=document.body;return b?Math.round(b.getBoundingClientRect().height):0;})()"
                    ) { value ->
                        val px = value?.trim()?.trim('"')?.toFloatOrNull() ?: return@evaluateJavascript
                        if (px <= 0f) return@evaluateJavascript
                        val next = (px + 16).toInt().coerceAtLeast(48)
                        // 加载过程中的像素级抖动不写回，避免带动下方评论区反复挪位置
                        if (!measured || kotlin.math.abs(next - contentHeightDp) >= 4) {
                            contentHeightDp = next
                        }
                        if (!measured) {
                            measured = true
                            onReady()
                        }
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: android.webkit.WebResourceRequest
                    ): Boolean = true // 外链一律不接管，由 Compose 侧决定

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        measureHeight()
                        // 图片/字体异步加载完成后再补测几次，避免长文带图底部被裁；
                        // 正文里的插图是 OSS 网络图，加载比文字慢，末尾再拉两档长延时
                        view.postDelayed({ measureHeight() }, 120)
                        view.postDelayed({ measureHeight() }, 400)
                        view.postDelayed({ measureHeight() }, 900)
                        view.postDelayed({ measureHeight() }, 1500)
                        view.postDelayed({ measureHeight() }, 2500)
                        view.postDelayed({ measureHeight() }, 4000)
                    }
                }
                loadDataWithBaseURL(null, document, "text/html", "UTF-8", null)
                // 记录已加载内容，避免同一文档被重复 load
                tag = document
            }
        },
        update = { webView ->
            if (webView.tag != document) {
                webView.tag = document
                webView.loadDataWithBaseURL(null, document, "text/html", "UTF-8", null)
            }
        }
    )

    // 折叠态在正文底部压一层渐隐，把被裁断的半行淡进背景，避免生硬的横向切口
    if (!expanded && overflow) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to fadeColor
                    )
                )
        )
    }
    }

    // 折叠时给「查看全部内容」，展开后给「收起」，长文随时能在两种状态间切换
    if (overflow) {
        TextButton(
            onClick = { if (expanded) onCollapse() else onExpand() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                if (expanded) "收起" else "查看全部内容",
                style = MaterialTheme.typography.bodySmall,
                color = Primary
            )
        }
    }
    }
}

/**
 * 正文 markdown 的最小转换，只覆盖发布页会写进去的语法：
 *  - `![说明](图片地址)` → `<img src="地址" alt="说明">`（插图）
 *  - 行首 `#` 到 `######` → h1..h4（标题）
 *  - 普通换行 → `<br>`，空行 → `<br>`
 *
 * 刻意不做完整 markdown 解析，也不做整体 HTML 转义，
 * 避免把正文里原有的英文/符号/已有标签误伤。
 */
private fun renderArticleMarkdown(src: String): String {
    val img = Regex("""!\[([^\]]*)]\(([^)\s]+)\)""")
    fun imgTag(alt: String, url: String): String =
        "<img src=\"$url\" alt=\"${alt.replace("\"", "&quot;")}\">"

    return buildString {
        for (raw in src.lines()) {
            val line = raw.trim()
            if (line.isEmpty()) {
                append("<br>")
                continue
            }
            // 整行就是一张图：直接出图，不补 <br>，避免图后多一截空白
            val whole = img.matchEntire(line)
            if (whole != null) {
                append(imgTag(whole.groupValues[1], whole.groupValues[2]))
                continue
            }
            // 行首标题
            val hashes = line.takeWhile { it == '#' }.length
            if (hashes in 1..6 && line.length > hashes) {
                val level = hashes.coerceAtMost(4)
                append("<h$level>").append(line.drop(hashes).trim()).append("</h$level>")
                continue
            }
            // 其余按普通段落：行内图片照样转，行尾补换行
            append(img.replace(line) { imgTag(it.groupValues[1], it.groupValues[2]) })
                .append("<br>")
        }
    }
}

@Composable
private fun CommentItem(
    comment: CommentDto,
    highlighted: Boolean = false,
    highlightReplyId: Long? = null,
    isMine: Boolean = false,
    myUserId: Long = 0L,
    onDelete: () -> Unit,
    onDeleteReply: (Long) -> Unit,
    onReply: () -> Unit = {},
    onReplyReply: (HotReplyDto) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlighted) HighlightWash else Color.Transparent)
            .padding(horizontal = if (highlighted) 8.dp else 0.dp)
    ) {
        // 评论正文点击即回复：默认 ripple 波纹保留，再补一个按压浅底，透明背景下反馈更可见
        val replyInteraction = remember { MutableInteractionSource() }
        val replyPressed by replyInteraction.collectIsPressedAsState()
        // 二级回复默认折叠，只露第一条，点入口再展开全部（同视频评论区）
        var repliesExpanded by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.Top) {
            Avatar(url = comment.userAvatar, name = comment.userName, size = 32.dp)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.userName.ifBlank { "匿名用户" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                MetaText(comment.createTime)
                Spacer(Modifier.height(6.dp))
                // 点击评论正文即进入回复，不再单独放「回复」按钮
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (replyPressed) surfaceWashBrush() else SolidColor(Color.Transparent))
                        .clickable(interactionSource = replyInteraction) { onReply() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            if (isMine) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "删除",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable(onClick = onDelete)
                )
            }
        }

        if (comment.hotReplies.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            val replies = comment.hotReplies
            // 通知定位高亮的回复若不在第一条，自动展开，避免目标被折叠藏住
            val highlightReply = replies.any { it.commentId == highlightReplyId }
            val visibleReplies = if (repliesExpanded || replies.size <= 1 || highlightReply) replies else replies.take(1)
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaceWashBrush())
                    .padding(10.dp)
            ) {
                visibleReplies.forEach { reply ->
                    val replyHighlighted = reply.commentId == highlightReplyId
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (replyHighlighted) HighlightWash else Color.Transparent)
                            .padding(horizontal = if (replyHighlighted) 6.dp else 0.dp)
                            .padding(vertical = 3.dp)
                            .clickable { onReplyReply(reply) }
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = reply.userName + if (reply.replyToName != null) " 回复 ${reply.replyToName}：" else "：",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Primary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = reply.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (reply.userId == myUserId) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "删除",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable { onDeleteReply(reply.commentId) }
                            )
                        }
                    }
                }
                if (replies.size > 1) {
                    Text(
                        text = if (repliesExpanded) "收起 ▴" else "查看全部 ${replies.size} 条回复 ▾",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clickable { repliesExpanded = !repliesExpanded }
                    )
                }
            }
        }

        // 二级回复默认折叠为第一条，点入口展开全部
    }
    Box(
        modifier = Modifier
            .padding(start = 56.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(Divider)
    )
}

@Composable
private fun ArticleBottomBar(
    likeCount: Int,
    commentCount: Int,
    onOpenComments: () -> Unit,
    isLiked: Boolean,
    isFavorited: Boolean,
    commentInput: String,
    onCommentChange: (String) -> Unit,
    replyToName: String?,
    onCancelReply: () -> Unit,
    onSendComment: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    /** false = 文章还没过审，评论框换成只读提示（后端评论接口只认已发布内容） */
    commentEnabled: Boolean = true
) {
    val barContext = androidx.compose.ui.platform.LocalContext.current
    val toast: (String) -> Unit = { msg ->
        android.widget.Toast.makeText(barContext, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
    Surface(shadowElevation = 8.dp) {
        Column {
            // 正在回复某人时给出明确提示，避免误以为还在发顶级评论
            if (replyToName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "回复 @$replyToName",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onCancelReply) {
                        Text("取消", color = TextHint, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!commentEnabled) {
                    // 审核中：不给输入框，避免用户敲完点发送再被后端打回
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(surfaceWashBrush())
                            .padding(horizontal = 14.dp)
                            .clickable { toast("文章审核通过后才能评论") },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "文章审核中，暂不能评论",
                            color = TextHint,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Button(
                        onClick = { toast("文章审核通过后才能评论") },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("发送")
                    }
                } else {
                BasicTextField(
                    value = commentInput,
                    onValueChange = onCommentChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(surfaceWashBrush())
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        if (commentInput.isEmpty()) {
                            Text(
                                text = if (replyToName != null) "回复 @$replyToName..." else "说点什么...",
                                color = TextHint,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        inner()
                    }
                )
                Button(
                    onClick = onSendComment,
                    enabled = commentInput.isNotBlank(),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("发送")
                }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BarAction(
                    icon = NavIcons.Favorite,
                    label = formatCount(likeCount),
                    tint = if (isLiked) Color(0xFFFF4D6A) else TextSecondary,
                    onClick = onToggleLike
                )
                BarAction(
                    icon = NavIcons.Comment,
                    label = formatCount(commentCount),
                    tint = TextSecondary,
                    onClick = onOpenComments
                )
                BarAction(
                    icon = NavIcons.Bookmark,
                    label = if (isFavorited) "已收藏" else "收藏",
                    tint = if (isFavorited) Primary else TextSecondary,
                    onClick = onToggleFavorite
                )
            }
        }
    }
}

@Composable
private fun BarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@Composable
private fun ReportDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    // 必须与接口 8.6 的 reason 枚举一致，否则后端校验会拒绝
    val reasons = listOf("广告骚扰", "色情低俗", "人身攻击", "违法违规", "其他")
    var selected by remember { mutableStateOf(reasons.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("举报文章") },
        text = {
            Column {
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selected = reason }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = reason,
                            color = if (selected == reason) Primary else TextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected == reason) {
                            Icon(NavIcons.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selected) }) { Text("提交") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 本人文章的审核状态条：作者能看到正文，这里说明为什么社区里搜不到 */
@Composable
private fun ArticleStatusNotice(status: Int) {
    val (text, color) = when (status) {
        PostStatus.PENDING -> "文章审核中，仅自己可见；审核通过后才会出现在社区" to Color(0xFFE08A1E)
        PostStatus.REJECTED -> "文章未通过审核，仅自己可见；修改后可重新提交" to MaterialTheme.colorScheme.error
        PostStatus.DRAFT -> "这篇文章还是草稿，仅自己可见" to TextSecondary
        else -> "文章已下架，仅自己可见" to TextSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, fontSize = 12.sp, color = color)
    }
}
