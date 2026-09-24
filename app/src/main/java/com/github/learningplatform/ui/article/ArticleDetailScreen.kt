package com.github.learningplatform.ui.article

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.ArticleDetailDto
import com.github.learningplatform.data.remote.dto.CommentDto
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
    var commentInput by remember { mutableStateOf("") }

    LaunchedEffect(articleId) { viewModel.load(articleId) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }

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
                                    viewModel.showNoteDialog()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("举报") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.showReport()
                                }
                            )
                            if (uiState.isMine) {
                                DropdownMenuItem(
                                    text = { Text("编辑") },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit(articleId)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            ArticleBottomBar(
                likeCount = uiState.detail?.likeCount ?: 0,
                commentCount = uiState.detail?.commentCount ?: 0,
                isLiked = uiState.isLiked,
                isFavorited = uiState.isFavorited,
                commentInput = commentInput,
                onCommentChange = { commentInput = it },
                onSendComment = {
                    if (commentInput.isNotBlank()) {
                        viewModel.postComment(commentInput)
                        commentInput = ""
                    }
                },
                onToggleLike = viewModel::toggleLike,
                onToggleFavorite = viewModel::toggleFavorite,
                onShare = viewModel::share
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
                LazyColumn(modifier = m.background(pageGradientBrush(strong = true)), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item {
                        ArticleHeader(
                            detail = detail,
                            onOpenAuthor = { onOpenAuthor(detail.authorId) },
                            onOpenTag = onOpenTag
                        )
                    }

                    item {
                        RichContent(
                            html = detail.content,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }

                    item { SectionHeader(title = "评论 (${detail.commentCount})") }

                    if (uiState.comments.isEmpty() && !uiState.commentsLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("还没有评论", color = TextHint, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    items(uiState.comments, key = { it.commentId }) { comment ->
                        CommentItem(
                            comment = comment,
                            onToggleLike = { viewModel.toggleCommentLike(comment.commentId) },
                            onDelete = { viewModel.deleteComment(comment.commentId) }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                detail.tags.take(5).forEach { tag ->
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
private fun RichContent(html: String, modifier: Modifier = Modifier) {
    if (html.isBlank()) {
        Text(
            text = "暂无正文",
            style = MaterialTheme.typography.bodyMedium,
            color = TextHint,
            modifier = modifier
        )
        return
    }

    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val textColor = if (dark) "#E0E0E0" else "#1A1A1A"
    val bgColor = if (dark) "#121212" else "#FFFFFF"

    val document = remember(html, dark) {
        """
        <!DOCTYPE html>
        <html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
          body { margin:0; padding:0; background:$bgColor; color:$textColor;
                 font-size:16px; line-height:1.75; word-break:break-word;
                 font-family:-apple-system, "PingFang SC", "Microsoft YaHei", sans-serif; }
          img, video { max-width:100%; height:auto; border-radius:8px; }
          table { width:100%; border-collapse:collapse; }
          td, th { border:1px solid #DDD; padding:6px; }
          pre { background:#F5F5F5; padding:10px; border-radius:8px; overflow-x:auto; }
          code { font-family:Consolas, monospace; }
          blockquote { margin:0; padding-left:12px; border-left:3px solid #1A73E8; color:#666; }
          a { color:#1A73E8; text-decoration:none; }
        </style></head>
        <body>$html</body></html>
        """.trimIndent()
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    // 初始给一个可见的最小高度，测量完成后按真实内容高度替换
    var contentHeightDp by remember { mutableIntStateOf(240) }

    AndroidView(
        modifier = modifier.height(contentHeightDp.dp),
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
                isVerticalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: android.webkit.WebResourceRequest
                    ): Boolean = true // 外链一律不接管，由 Compose 侧决定

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        view.evaluateJavascript(
                            "(function(){return Math.ceil(document.body.scrollHeight);})()"
                        ) { value ->
                            val px = value?.trim()?.trim('"')?.toFloatOrNull() ?: return@evaluateJavascript
                            if (px > 0f) {
                                val dp = with(density) { px.toDp().value }.toInt()
                                if (dp > 0) contentHeightDp = dp.coerceAtLeast(120)
                            }
                        }
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
}

@Composable
private fun CommentItem(
    comment: CommentDto,
    onToggleLike: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(url = comment.userAvatar, name = comment.userName, size = 32.dp)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comment.userName.ifBlank { "匿名用户" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                MetaText(comment.createTime)
            }
            Text(
                text = "${formatCount(comment.likeCount)} 赞",
                style = MaterialTheme.typography.labelSmall,
                color = if (comment.isLiked) Primary else TextHint,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (comment.isLiked) Primary.copy(alpha = 0.1f) else SurfaceColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .clickable(onClick = onToggleLike)
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = comment.content,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )

        if (comment.hotReplies.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaceWashBrush())
                    .padding(10.dp)
            ) {
                comment.hotReplies.forEach { reply ->
                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                        Text(
                            text = reply.userName + if (reply.replyToName != null) " 回复 ${reply.replyToName}：" else "：",
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = reply.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        if (comment.replyCount > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "共 ${comment.replyCount} 条回复",
                style = MaterialTheme.typography.labelSmall,
                color = Primary
            )
        }
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
    isLiked: Boolean,
    isFavorited: Boolean,
    commentInput: String,
    onCommentChange: (String) -> Unit,
    onSendComment: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            Text("说点什么...", color = TextHint, style = MaterialTheme.typography.bodyMedium)
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
                    onClick = { }
                )
                BarAction(
                    icon = NavIcons.Bookmark,
                    label = if (isFavorited) "已收藏" else "收藏",
                    tint = if (isFavorited) Primary else TextSecondary,
                    onClick = onToggleFavorite
                )
                BarAction(
                    icon = NavIcons.Share,
                    label = "分享",
                    tint = TextSecondary,
                    onClick = onShare
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
