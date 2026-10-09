package com.github.learningplatform.ui.course

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseId: Long,
    onBack: () -> Unit,
    onOpenPlayer: (Long, Long?) -> Unit,
    viewModel: CourseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(courseId) { viewModel.load(courseId) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val detail = uiState.detail
            if (detail != null) {
                CourseBottomBar(
                    joined = detail.isJoined,
                    isFree = detail.isFree == 1,
                    busy = uiState.joining,
                    isFavorited = uiState.isFavorited,
                    onJoin = viewModel::joinOrPlay,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onPlay = {
                        if (!uiState.hasPlayPermission(detail)) {
                            viewModel.notifyNeedJoin()
                        } else {
                            onOpenPlayer(courseId, null)
                        }
                    }
                )
            }
        }
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
                // 【任务④：下拉强制回源】内容外层包 Material3 下拉刷新手势：
                // 下拉触发 viewModel.refresh()（forceRefresh=true，跳过缓存直接回源并覆盖缓存）。
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = m.background(pageGradientBrush(strong = true))
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
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

                    item { CourseHeader(detail = uiState.detail!!, isFavorited = uiState.isFavorited) }

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

                    item { CourseTabs(selected = uiState.tab, onSelect = viewModel::selectTab) }

                    when (uiState.tab) {
                        0 -> catalogSection(detail.chapters, onOpenPlayer = { lesson ->
                            // 免费试看小节即使未加入也放行（6.3）
                            if (!viewModel.uiState.value.hasPlayPermission(detail, lesson.isFree == 1)) {
                                viewModel.notifyNeedJoin()
                            } else {
                                onOpenPlayer(courseId, lesson.lessonId)
                            }
                        })
                        else -> ratingSection(uiState.ratings, uiState.myScore, viewModel::rate)
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseHeader(
    detail: com.github.learningplatform.data.remote.dto.CourseDetailDto,
    isFavorited: Boolean
) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(url = "", name = detail.instructorName, size = 32.dp)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = detail.instructorName.ifBlank { "未知讲师" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                MetaText(
                    text = String.format(
                        Locale.CHINA,
                        "%.1f 分 · %d 人评价 · %s 人在学",
                        detail.averageScore,
                        detail.ratingCount,
                        formatCount(detail.studentCount)
                    )
                )
            }
            Text(
                text = if (isFavorited) "已收藏" else "收藏",
                style = MaterialTheme.typography.bodySmall,
                color = Primary
            )
        }
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
    onOpenPlayer: (CourseLessonDto) -> Unit
) {
    if (chapters.isEmpty()) {
        item { EmptyState(message = "暂无章节", modifier = Modifier.height(200.dp)) }
        return
    }
    chapters.forEach { chapter ->
        item(key = "chapter-${chapter.chapterId}") {
            ChapterBlock(chapter = chapter, onOpenPlayer = onOpenPlayer)
        }
    }
}

@Composable
private fun ChapterBlock(
    chapter: CourseChapterDto,
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
                LessonRow(lesson = lesson, onOpenPlayer = onOpenPlayer)
                HorizontalDivider(color = Divider, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
}

@Composable
private fun LessonRow(
    lesson: CourseLessonDto,
    onOpenPlayer: (CourseLessonDto) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenPlayer(lesson) }
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
    onRate: (Double) -> Unit
) {
    item {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "我的评分",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 【任务⑤：key 稳定】静态常量列表也补上稳定 key（评分值本身即唯一键）
                items((1..10).map { it * 0.5 }, key = { it }) { score ->
                    val selected = myScore > 0 && kotlin.math.abs(myScore - score) < 0.01
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) Primary else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) Primary else Divider
                        ),
                        modifier = Modifier.clickable { onRate(score) }
                    ) {
                        Text(
                            text = String.format(Locale.CHINA, "%.1f", score),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selected) Color.White else TextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    if (ratings.isEmpty()) {
        item { EmptyState(message = "暂无评价", modifier = Modifier.height(200.dp)) }
    } else {
        item { SectionHeader(title = "全部评价") }
        items(ratings, key = { "rating-${it.userId}-${it.score}-${it.createTime}" }) { rating ->
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
                        Text(
                            text = String.format(Locale.CHINA, "%.1f 分", rating.score),
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary
                        )
                    }
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
