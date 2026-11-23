package com.github.learningplatform.ui.mine

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.LearningCourseDto
import com.github.learningplatform.data.remote.dto.StudyStatsDto
import com.github.learningplatform.data.remote.dto.WeekMinuteDto
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.NetImage
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.surfaceWashBrush

/**
 * 学习数据页（后端 GET /api/v1/learn/stats）。
 *
 * 只统计两类内容：课程播放、文章阅读；短视频不计入。
 * 结构：今日概览（总量 + 课程/阅读构成）→ 本周趋势（堆叠）→ 课程学习（统计 + 在学/已完成课程）
 * → 阅读与笔记。
 *
 * 课程列表来自 GET /api/v1/learn/courses，顶部「在学 / 已完成」切换；点课程进播放器。
 *
 * 分来源字段后端还没返回时自动降级：只显示合计，不显示构成条与拆分区。
 */
@Composable
fun StudyStatsScreen(
    onBack: () -> Unit,
    onOpenCourse: (Long) -> Unit = {},
    onOpenReadingHistory: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    viewModel: StudyStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 页面回到前台（从课程详情 / 播放器返回、切回 App）时静默刷新，
    // 否则看到的仍是进入页面那一刻的统计与课程进度。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // 首次进入由 init 加载；返回 / 切回前台时补一次静默刷新
                    if (viewModel.uiState.value.stats != null) viewModel.refresh()
                    // 停留在页面期间定时刷新，离开页面即停
                    viewModel.startPolling()
                }
                Lifecycle.Event.ON_PAUSE -> viewModel.stopPolling()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.stopPolling()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = false))
    ) {
        BackTopBar(title = "学习数据", onBack = onBack)

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(
                message = uiState.error.orEmpty(),
                onRetry = { viewModel.load() }
            )
            uiState.stats == null -> EmptyState(message = "暂无学习数据")
            else -> StatsContent(
                state = uiState,
                onStatusChange = { viewModel.setCourseStatus(it) },
                onRetryCourses = { viewModel.loadCourses(uiState.courseStatus) },
                onOpenCourse = onOpenCourse,
                onOpenReadingHistory = onOpenReadingHistory,
                onOpenNotes = onOpenNotes
            )
        }
    }
}

@Composable
private fun StatsContent(
    state: StudyStatsUiState,
    onStatusChange: (String) -> Unit,
    onRetryCourses: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    onOpenReadingHistory: () -> Unit,
    onOpenNotes: () -> Unit
) {
    val stats = state.stats ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { TodayCard(stats) }
        item { WeekSection(stats.weekMinutes) }
        item {
            CourseSection(
                stats = stats,
                courseStatus = state.courseStatus,
                courses = state.courses,
                coursesLoading = state.coursesLoading,
                coursesError = state.coursesError,
                onStatusChange = onStatusChange,
                onRetry = onRetryCourses,
                onOpenCourse = onOpenCourse
            )
        }
        item { ReadingSection(stats, onOpenReadingHistory, onOpenNotes) }
        item { Footnote() }
    }
}

/** 统一卡片容器：16dp 圆角 + 主题覆盖层 */
@Composable
private fun StatsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceWashBrush())
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun TodayCard(stats: StudyStatsDto) {
    StatsCard {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text("今日学习", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${stats.todayMinutes}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("分钟", fontSize = 12.sp, color = TextHint)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("连续学习", fontSize = 12.sp, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${stats.continuousDays} 天",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary
                )
            }
        }

        val courseMinutes = stats.todayCourseMinutes
        val articleMinutes = stats.todayArticleMinutes
        if (courseMinutes != null && articleMinutes != null) {
            Spacer(Modifier.height(16.dp))
            SplitBar(
                courseMinutes = courseMinutes,
                articleMinutes = articleMinutes,
                height = 8.dp
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(Primary, "课程 $courseMinutes 分")
                Spacer(Modifier.width(14.dp))
                LegendDot(ReadingColor, "阅读 $articleMinutes 分")
            }
        }
    }
}

/** 课程 / 阅读两段占比条；两边都为 0 时只画底槽 */
@Composable
private fun SplitBar(courseMinutes: Int, articleMinutes: Int, height: androidx.compose.ui.unit.Dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (courseMinutes > 0) {
            Box(
                modifier = Modifier
                    .weight(courseMinutes.toFloat())
                    .fillMaxHeight()
                    .background(Primary)
            )
        }
        if (articleMinutes > 0) {
            Box(
                modifier = Modifier
                    .weight(articleMinutes.toFloat())
                    .fillMaxHeight()
                    .background(ReadingColor)
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun WeekSection(weekMinutes: List<WeekMinuteDto>) {
    StatsCard {
        Text("本周趋势", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        if (weekMinutes.isEmpty()) {
            Text("暂无本周数据", fontSize = 12.sp, color = TextHint)
            return@StatsCard
        }

        val maxMinutes = weekMinutes.maxOfOrNull { it.minutes }?.coerceAtLeast(1) ?: 1
        val hasSplit = weekMinutes.any { it.courseMinutes != null && it.articleMinutes != null }

        weekMinutes.forEach { day ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    day.weekday.ifBlank { day.date.takeLast(5) },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.width(28.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(day.minutes.toFloat() / maxMinutes)
                            .fillMaxHeight()
                    ) {
                        val course = day.courseMinutes
                        val article = day.articleMinutes
                        if (hasSplit && course != null && article != null) {
                            if (course > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(course.toFloat())
                                        .fillMaxHeight()
                                        .background(Primary)
                                )
                            }
                            if (article > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(article.toFloat())
                                        .fillMaxHeight()
                                        .background(ReadingColor)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Primary)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "${day.minutes} 分",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    modifier = Modifier.width(48.dp)
                )
            }
        }

        if (hasSplit) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(Primary, "课程")
                Spacer(Modifier.width(14.dp))
                LegendDot(ReadingColor, "阅读")
            }
        }
    }
}

@Composable
private fun CourseSection(
    stats: StudyStatsDto,
    courseStatus: String,
    courses: List<LearningCourseDto>?,
    coursesLoading: Boolean,
    coursesError: String?,
    onStatusChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenCourse: (Long) -> Unit
) {
    StatsCard {
        Text("课程学习", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell("已完成", "${stats.completedCourses} 门", Modifier.weight(1f))
            StatCell(
                "在学",
                stats.learningCourses?.let { "$it 门" } ?: "—",
                Modifier.weight(1f)
            )
            StatCell(
                "累计时长",
                stats.totalCourseMinutes?.let { formatMinutes(it) } ?: "—",
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CourseTab("在学", courseStatus == CourseStatus.LEARNING) {
                onStatusChange(CourseStatus.LEARNING)
            }
            Spacer(Modifier.width(8.dp))
            CourseTab("已完成", courseStatus == CourseStatus.COMPLETED) {
                onStatusChange(CourseStatus.COMPLETED)
            }
        }
        Spacer(Modifier.height(6.dp))

        when {
            coursesLoading -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("加载课程中", fontSize = 12.sp, color = TextSecondary)
            }

            coursesError != null -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    coursesError,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "重试",
                    fontSize = 12.sp,
                    color = Primary,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable(onClick = onRetry)
                )
            }

            courses.isNullOrEmpty() -> Text(
                if (courseStatus == CourseStatus.LEARNING) "还没有在学的课程，去首页挑一门开始吧"
                else "还没有学完的课程",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            else -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                courses.forEach { course ->
                    CourseRow(course) { onOpenCourse(course.courseId) }
                }
            }
        }
    }
}

@Composable
private fun CourseTab(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        color = if (active) Primary else TextSecondary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) Primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

/** 课程行：封面 + 标题 + 进度条 + 已完成小节数 + 最近学习小节；点击进播放器 */
@Composable
private fun CourseRow(course: LearningCourseDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NetImage(
            url = course.coverUrl,
            modifier = Modifier
                .width(72.dp)
                .height(54.dp),
            radius = 10.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                course.courseTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(7.dp))
            val ratio = (course.progressPercent / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (ratio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .fillMaxHeight()
                            .background(Primary)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "已完成 ${course.finishedLessons}/${course.totalLessons} 小节",
                fontSize = 11.sp,
                color = TextSecondary
            )
            if (course.lastLessonTitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    "上次：${course.lastLessonTitle}",
                    fontSize = 11.sp,
                    color = TextHint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("›", fontSize = 18.sp, color = TextHint)
    }
}

@Composable
private fun ReadingSection(
    stats: StudyStatsDto,
    onOpenReadingHistory: () -> Unit,
    onOpenNotes: () -> Unit
) {
    StatsCard {
        Text("阅读与笔记", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                "本周阅读",
                stats.articleMinutesWeek?.let { "$it 分" } ?: "—",
                Modifier.weight(1f)
            )
            StatCell(
                "本周篇数",
                stats.readPostsWeek?.let { "$it 篇" } ?: "—",
                Modifier.weight(1f)
            )
            StatCell("笔记", "${stats.noteCount} 条", Modifier.weight(1f))
        }
        val todayPosts = stats.readPostsToday
        if (todayPosts != null) {
            Spacer(Modifier.height(12.dp))
            Text("今日已读 $todayPosts 篇", fontSize = 12.sp, color = TextSecondary)
        }
        Spacer(Modifier.height(12.dp))
        // 统计只给数字，具体读过的文章 / 写过的笔记从这里进：
        // 阅读记录 → 观看历史的文章 tab；笔记 → 笔记本（默认就是文章笔记）
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "阅读记录",
                fontSize = 12.sp,
                color = Primary,
                modifier = Modifier.clickable(onClick = onOpenReadingHistory)
            )
            Text(" ›", fontSize = 12.sp, color = Primary)
            Spacer(Modifier.weight(1f))
            Text(
                "我的笔记",
                fontSize = 12.sp,
                color = Primary,
                modifier = Modifier.clickable(onClick = onOpenNotes)
            )
            Text(" ›", fontSize = 12.sp, color = Primary)
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun Footnote() {
    Text(
        "统计口径：课程播放 + 文章阅读，短视频不计入；分钟按每天累计秒数四舍五入。",
        fontSize = 11.sp,
        color = TextHint,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    )
}

/** 阅读用浅一档的主色，保证深色模式下也能和课程区分 */
private val ReadingColor = Primary.copy(alpha = 0.35f)

private fun formatMinutes(minutes: Int): String {
    if (minutes < 60) return "$minutes 分"
    val hours = minutes / 60
    val rest = minutes % 60
    return if (rest == 0) "$hours 小时" else "$hours 小时 $rest 分"
}
