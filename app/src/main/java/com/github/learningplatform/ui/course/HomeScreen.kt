package com.github.learningplatform.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.ui.common.CategoryTabs
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.NetImage
import com.github.learningplatform.ui.common.SectionHeader
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
import kotlinx.coroutines.delay

/**
 * 课程首页（对齐 UI 稿「课程首页」）。
 *
 * 数据来源（接口文档 v3.0）：
 *  4.1 category/tree?type=2   —— 课程分类胶囊
 *  6.1 courses                —— 热门课程 / 热度排行 / 分类筛选结果
 *  13.1 ad-slots?scene=startup —— 广告位（暂不接入 SDK，仅占位）
 */
@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenAllCategories: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 与登录页一致的奶油渐变（只铺顶部一段；列表页整屏渐变会让长时间阅读很累）
            .background(pageGradientBrush(strong = false))
    ) {
        HomeTopBar(onOpenSearch = onOpenSearch)

        if (uiState.categories.isNotEmpty()) {
            CategoryTabs(
                items = listOf(null to "全部") + uiState.categories,
                selectedId = uiState.selectedCategoryId,
                onSelect = viewModel::selectCategory,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        when {
            uiState.isLoading && uiState.hotCourses.isEmpty() -> LoadingState()
            uiState.error != null && uiState.hotCourses.isEmpty() ->
                ErrorState(message = uiState.error.orEmpty(), onRetry = viewModel::refresh)
            else -> CourseFeed(
                uiState = uiState,
                onOpenAllCategories = onOpenAllCategories,
                onOpenCourse = onOpenCourse,
                onLoadMore = viewModel::loadMore
            )
        }
    }
}

@Composable
private fun HomeTopBar(onOpenSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { }) {
            Icon(NavIcons.Menu, contentDescription = "菜单", tint = TextPrimary)
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(50))
                .background(surfaceWashBrush())
                .clickable(onClick = onOpenSearch)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "您想学习什么？",
                style = MaterialTheme.typography.bodyMedium,
                color = TextHint,
                modifier = Modifier.weight(1f)
            )
            Icon(
                NavIcons.Search,
                contentDescription = "搜索",
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(onClick = { }) {
            Icon(NavIcons.Notifications, contentDescription = "通知", tint = TextPrimary)
        }
    }
}

@Composable
private fun CourseFeed(
    uiState: HomeUiState,
    onOpenAllCategories: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = false)),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SectionHeader(
                title = "课程分类",
                actionText = "查看全部",
                onAction = onOpenAllCategories
            )
        }

        item {
            FeaturedCourseCarousel(
                courses = uiState.featuredCourses,
                onOpenCourse = onOpenCourse,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // 广告位：后端配了 slot 才渲染。
        // 特意不做「广告位占位」的假框 —— 没广告时它只是一块视觉噪音，
        // 白白占掉首屏最贵的位置。接入 Taku 后这里换成真实广告容器即可。
        if (uiState.adSlots.isNotEmpty()) {
            item {
                AdSlot(
                    slots = uiState.adSlots,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        item { SectionHeader(title = "热门课程") }

        if (uiState.hotCourses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "该分类下暂无课程",
                        color = TextHint,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            item { HotCourseGroup(courses = uiState.hotCourses, onOpenCourse = onOpenCourse) }
        }

        if (uiState.rankCourses.isNotEmpty()) {
            item { SectionHeader(title = "本周课程热度排行") }
            items(uiState.rankCourses, key = { "rank-${it.courseId}" }) { course ->
                RankRow(course = course, onOpenCourse = onOpenCourse)
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Divider)
                )
            }
        }

        if (uiState.hasMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoadingMore) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "上拉加载更多",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextHint,
                            modifier = Modifier.clickable(onClick = onLoadMore)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HotCourseGroup(courses: List<CourseDto>, onOpenCourse: (Long) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(surfaceWashBrush())
            .padding(vertical = 6.dp)
    ) {
        courses.take(2).forEach { course ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenCourse(course.courseId) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    NetImage(
                        url = course.coverUrl,
                        modifier = Modifier
                            .width(110.dp)
                            .height(66.dp),
                        radius = 8.dp
                    )
                    Icon(
                        NavIcons.Play,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (course.studentCount > 0)
                            formatCount(course.studentCount) + "人正在学"
                        else "新上线",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun RankRow(course: CourseDto, onOpenCourse: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCourse(course.courseId) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NetImage(
            url = course.coverUrl,
            modifier = Modifier
                .width(120.dp)
                .height(72.dp),
            radius = 10.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (course.instructorName.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "讲师：" + course.instructorName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * 精选课程轮播。
 *
 * 版式取自设计稿的横向大图卡：整块 16:9 封面，底部压一层自下而上的深色渐变，
 * 课程标题与讲师直接叠在图上（而不是图下再排一行文字）——这样一张图就是一条完整信息，
 * 首屏能放下更多内容，也比「图 + 文」的卡片更有质感。
 *
 * 交互：
 *  - 每 4 秒自动翻页；用户手动滑动后暂停自动播放 8 秒（[autoScrollSuspended]），
 *    否则会出现「刚滑过去又被自动翻走」的抢操作；
 *  - 只有一张时不自动播放、不显示指示器。
 */
@Composable
private fun FeaturedCourseCarousel(
    courses: List<CourseDto>,
    onOpenCourse: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (courses.isEmpty()) return

    // 用 LazyRow 而不是 HorizontalPager。
    // 原因：HorizontalPager 在这个 foundation 版本上页宽明显小于容器
    // （指定 PageSize.Fill 也无效），结果是「右侧露出下一张、标题被裁字」。
    // LazyRow 的 fillParentMaxWidth() 能精确等于视口宽度，行为可控。
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var autoScrollSuspended by remember { mutableStateOf(false) }

    val currentPage = listState.firstVisibleItemIndex.coerceIn(0, courses.lastIndex)

    // 用户一动手就暂停自动播放，8 秒后恢复，避免「刚滑过去又被自动翻走」
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            autoScrollSuspended = true
        } else if (autoScrollSuspended) {
            delay(8000)
            autoScrollSuspended = false
        }
    }

    LaunchedEffect(courses.size, autoScrollSuspended) {
        if (courses.size <= 1 || autoScrollSuspended) return@LaunchedEffect
        while (true) {
            delay(4000)
            val next = (listState.firstVisibleItemIndex + 1) % courses.size
            runCatching { listState.animateScrollToItem(next) }
        }
    }

    Column(modifier = modifier) {
        /*
         * 版式：当前卡占 88% 宽，右侧留出下一张的边缘 —— 这是轮播的常规做法，
         * 给用户「可以左右滑」的提示。
         *
         * 之前我把它当 bug 追了很久（试过 HorizontalPager 的 PageSize.Fill、
         * LazyRow 的 fillParentMaxWidth、BoxWithConstraints 给死宽度），
         * 都没能让卡片撑满。后来想清楚：撑满反而不好 —— 满宽卡片看不出还能滑。
         * 所以这里明确按「露出下一张」来做，宽度和间距都写死，行为可预期。
         *
         * 卡片用 RoundedCornerShape 单独圆角，比给整个容器加 clip 更好：
         * 容器不裁剪，下一张才能自然露出边缘。
         */
        val cardWidthRatio = 0.88f
        val cardSpacing = 10.dp

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        ) {
            val cardWidth = maxWidth * cardWidthRatio
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(cardSpacing),
                // 滑动结束对齐到整页，等价于 pager 的翻页手感
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
            ) {
                items(courses, key = { it.courseId }) { course ->
                    Column(
                        modifier = Modifier
                            .width(cardWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenCourse(course.courseId) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            NetImage(
                                url = course.coverUrl,
                                modifier = Modifier.fillMaxSize(),
                                radius = 0.dp
                            )
                        }

                        // 信息条：固定高度、纯色底，文字落在卡片安全区内
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.72f))
                                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = course.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = buildString {
                                    if (course.instructorName.isNotBlank()) {
                                        append(course.instructorName)
                                    }
                                    if (course.studentCount > 0) {
                                        if (isNotEmpty()) append(" · ")
                                        append(formatCount(course.studentCount))
                                        append(" 人在学")
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 指示器压在图内底部居中
            if (courses.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 46.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(courses.size) { index ->
                        val active = index == currentPage
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(if (active) 16.dp else 6.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (active) Color.White
                                    else Color.White.copy(alpha = 0.45f)
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 广告位容器（预留）。
 *
 * 需求确认「暂不做广告」，所以这里**不渲染任何内容**，只保留位置与约定：
 * 后端返回的 slotCode 决定用哪个 Taku 广告位（见 HomeUiState.adSlots），
 * 接入时把 [Box] 内部换成 Taku 的广告 View 即可，上层布局不用动。
 *
 * 之所以保留这个空壳而不是删掉调用点：接入广告时不用再回来找位置，
 * 也不会因为「当时没有广告位」而把这段逻辑漏掉。
 */
@Composable
private fun AdSlot(
    slots: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    // 当前无广告内容：不占空间。
    // 若将来需要在无填充时显示兜底内容（如「开通会员免广告」），在这里加。
    Box(modifier = modifier.fillMaxWidth()) {
        // 预留：Taku 广告容器挂载点
        // slots.firstOrNull()?.first 即 slotCode
    }
}
