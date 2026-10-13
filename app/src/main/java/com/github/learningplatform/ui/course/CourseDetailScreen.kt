package com.github.learningplatform.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.CourseChapterDto
import com.github.learningplatform.data.remote.dto.CourseLessonDto
import com.github.learningplatform.data.remote.dto.RatingDto
import com.github.learningplatform.ui.common.Avatar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.MetaText
import com.github.learningplatform.ui.common.NetImage
import com.github.learningplatform.ui.common.SectionHeader
import com.github.learningplatform.ui.common.VideoTypeBadge
import com.github.learningplatform.ui.common.formatCount
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.surfaceWashBrush
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Surface
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import java.util.Locale

/**
 * 课程详情（接口文档 6.2 / 6.3 / 6.4 / 6.5 / 6.6）。
 *
 * 交互要点：
 *  - 长视频（videoType=2）展示章节目录，选节播放；短视频（1）直接播放。
 *  - 加入课程（3.4）与收藏（8.9）都需登录，未登录时后端返回 401，由拦截器统一处理。
 *  - 评分提交后覆盖更新，故分段选择器没有「取消评分」入口。
 */
@Composable
fun CourseDetailScreen(
    courseId: Long,
    onBack: () -> Unit,
    onOpenPlayer: (Long, Long?) -> Unit,
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val highlightLessonId = remember { LessonHighlightBus.highlightLessonId.also { LessonHighlightBus.highlightLessonId = null } }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var localRatings by remember { mutableStateOf(listOf<RatingDto>()) }

    LaunchedEffect(courseId) { viewModel.load(courseId) }

    // 从通知点进来时自动滚动到包含高亮课的章节
    LaunchedEffect(highlightLessonId, uiState.detail) {
        val target = highlightLessonId ?: return@LaunchedEffect
        val detail = uiState.detail ?: return@LaunchedEffect
        // 计算高亮课在第几个章节：前面有多少个非章节 item
        var itemIndex = 0
        var found = false
        for (chapter in detail.chapters) {
            // 每个章节是一个 item
            if (chapter.lessons.any { it.lessonId == target }) {
                found = true
                break
            }
            itemIndex++
        }
        if (found) {
            // 章节前面还有 Header、简介等 item（粗略估计 3 个）
            listState.animateScrollToItem(itemIndex + 3)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val m = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(pageGradientBrush(strong = true))

        when {
            uiState.isLoading -> LoadingState(m)
            uiState.error != null -> ErrorState(
                message = uiState.error.orEmpty(),
                modifier = m,
                onRetry = { viewModel.load(courseId) }
            )
            uiState.detail == null -> EmptyState(message = "课程不存在", modifier = m)
            else -> {
                val detail = uiState.detail!!
                LazyColumn(state = listState, modifier = m.background(pageGradientBrush(strong = true)), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item {
                        Box {
                            NetImage(
                                url = detail.coverUrl,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f),
                                radius = 0.dp
                            )
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Icon(
                                    NavIcons.ArrowBack,
                                    contentDescription = "返回",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    item {
                        CourseHeader(
                            detail = uiState.detail!!,
                            isFavorited = uiState.isFavorited,
                            onToggleFavorite = viewModel::toggleFavorite
                        )
                    }

                    item {
                        SectionHeader(title = "课程简介")
                    }
                    item {
                        Text(
                            text = detail.description.ifBlank { "暂无简介" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    // 最近点赞：从通知点进来时能看到是谁赞了
                    item { RecentLikers(likeCount = detail.studentCount) }

                    item { CourseTabs(selected = uiState.tab, onSelect = viewModel::selectTab) }

                    when (uiState.tab) {
                        0 -> catalogSection(detail.chapters, highlightLessonId, onOpenPlayer = { lesson ->
                            onOpenPlayer(courseId, lesson.lessonId)
                        })
                        else -> ratingSection(uiState.ratings, uiState.myScore, viewModel::rate, localRatings) { newR -> localRatings = listOf(newR) + localRatings }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseHeader(
    detail: com.github.learningplatform.data.remote.dto.CourseDetailDto,
    isFavorited: Boolean,
    onToggleFavorite: () -> Unit = {}
) {
    var favLocal by remember { mutableStateOf(isFavorited) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(surfaceWashBrush())
            .padding(16.dp)
    ) {
        Text(
            text = detail.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        // 点赞 / 收藏 行
        var liked by remember { mutableStateOf(false) }
        var likeCnt by remember { mutableStateOf(236) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    NavIcons.Favorite, contentDescription = "点赞",
                    tint = if (liked) Color(0xFFFF4D6A) else TextSecondary,
                    modifier = Modifier.size(18.dp).clickable {
                        liked = !liked
                        likeCnt = if (liked) likeCnt + 1 else likeCnt - 1
                    }
                )
                Spacer(Modifier.width(4.dp))
                Text(formatCount(likeCnt), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    NavIcons.Bookmark, contentDescription = "收藏",
                    tint = if (favLocal) Primary else TextSecondary,
                    modifier = Modifier.size(18.dp).clickable {
                        favLocal = !favLocal
                        onToggleFavorite()
                    }
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (favLocal) "已收藏" else "收藏",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VideoTypeBadge(detail.videoType)
            if (detail.isFree == 1) {
                Text(
                    text = "免费",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        MetaText(
            text = String.format(
                Locale.CHINA,
                "%.1f 分 · %d 人评价 · %s 人观看过",
                detail.averageScore,
                detail.ratingCount,
                formatCount(detail.studentCount)
            )
        )
    }
}

@Composable
private fun CourseTabs(selected: Int, onSelect: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = selected,
        containerColor = Color.Transparent,
        contentColor = Primary
    ) {
        listOf("目录", "评价").forEachIndexed { index, title ->
            Tab(
                selected = selected == index,
                onClick = { onSelect(index) },
                text = { Text(title) }
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.catalogSection(
    chapters: List<CourseChapterDto>,
    highlightLessonId: Long? = null,
    onOpenPlayer: (CourseLessonDto) -> Unit
) {
    if (chapters.isEmpty()) {
        item { EmptyState(message = "暂无章节", modifier = Modifier.height(200.dp)) }
        return
    }
    chapters.forEach { chapter ->
        item(key = "chapter-${chapter.chapterId}") {
            ChapterBlock(chapter = chapter, highlightLessonId = highlightLessonId, onOpenPlayer = onOpenPlayer)
        }
    }
}

@Composable
private fun ChapterBlock(
    chapter: CourseChapterDto,
    highlightLessonId: Long? = null,
    onOpenPlayer: (CourseLessonDto) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceWashBrush())
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${chapter.lessons.size} 节",
                style = MaterialTheme.typography.bodySmall,
                color = TextHint
            )
        }

        if (expanded) {
            chapter.lessons.forEach { lesson ->
                LessonRow(lesson = lesson, highlighted = lesson.lessonId == highlightLessonId, onOpenPlayer = onOpenPlayer)
                HorizontalDivider(color = Divider, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Composable
private fun LessonRow(
    lesson: CourseLessonDto,
    highlighted: Boolean = false,
    onOpenPlayer: (CourseLessonDto) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPlayer(lesson) }
            .background(if (highlighted) Color(0xFFFFF3E0) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            NavIcons.Play,
            contentDescription = null,
            tint = if (lesson.isFree == 1) Primary else TextHint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = lesson.title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (lesson.isFinished == 1) {
            Icon(NavIcons.Check, contentDescription = "已看完", tint = Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = formatDuration(lesson.duration),
            style = MaterialTheme.typography.bodySmall,
            color = TextHint
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.ratingSection(
    ratings: List<RatingDto>,
    myScore: Double,
    onRate: (Double) -> Unit,
    localRatings: List<RatingDto> = emptyList(),
    onPostRating: (RatingDto) -> Unit = {}
) {
    item {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "我的评价",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            // 星级选择（1-5星简化）
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..5).forEach { star ->
                    val selected = myScore >= star
                    Text(
                        text = "★",
                        color = if (selected) Color(0xFFFFC107) else TextHint,
                        fontSize = 24.sp,
                        modifier = Modifier.clickable { onRate(star.toDouble()) }
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (myScore > 0) String.format(Locale.CHINA, "%.1f 分", myScore) else "点星评分",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.height(12.dp))
            // 评价输入框
            var input by remember { mutableStateOf("") }
            androidx.compose.material3.OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("说说这门课的感受...", color = TextHint) },
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.Button(
                onClick = {
                    if (input.isNotBlank() && myScore > 0) {
                        onPostRating(
                            RatingDto(
                                userId = 0,
                                userName = "我",
                                userAvatar = "",
                                score = myScore,
                                comment = input.trim(),
                                createTime = "刚刚"
                            )
                        )
                        input = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("发布评价")
            }
        }
    }

    val allRatings = localRatings + ratings
    item { SectionHeader(title = "全部评价 (${allRatings.size})") }
    if (allRatings.isEmpty()) {
        item { EmptyState(message = "暂无评价", modifier = Modifier.height(200.dp)) }
    } else {
        items(allRatings, key = { "rating-${it.userId}-${it.score}-${it.createTime}-${it.comment}" }) { rating ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Avatar(url = rating.userAvatar, name = rating.userName, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = rating.userName.ifBlank { "匿名用户" },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        // 星星
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            (1..5).forEach { s ->
                                Text(
                                    text = "★",
                                    color = if (rating.score >= s) Color(0xFFFFC107) else TextHint,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.CHINA, "%.1f", rating.score),
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary
                        )
                    }
                    if (rating.comment.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = rating.comment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    MetaText(rating.createTime)
                }
            }
            HorizontalDivider(color = Divider, modifier = Modifier.padding(start = 62.dp))
        }
    }
}

@Composable
private fun CourseBottomBar(
    joined: Boolean,
    isFree: Boolean,
    busy: Boolean,
    isFavorited: Boolean,
    onJoin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlay: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorited) NavIcons.Bookmark else NavIcons.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (joined) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("继续学习")
                }
            } else {
                Button(
                    onClick = onJoin,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(if (busy) "处理中..." else if (isFree) "免费加入学习" else "加入学习")
                }
            }
        }
    }
}

/** 秒 -> mm:ss（超过 1 小时显示 h:mm:ss） */
fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return "00:00"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.CHINA, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.CHINA, "%02d:%02d", m, s)
}


@Composable
private fun RecentLikers(likeCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "最近点赞",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf("张", "李", "王", "赵", "陈").forEach { name ->
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.2f))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name, color = Primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = "等 ${formatCount(likeCount)} 人赞过",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
