package com.github.learningplatform.ui.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.BannerDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.BuildConfig
import com.github.learningplatform.ui.ad.TakuNativeAdCard
import com.github.learningplatform.ui.axing.AxingFloatingEntry
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
import java.util.Locale
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
    onOpenNotifications: () -> Unit,
    onOpenAllCategories: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    onOpenAxing: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // 首页：藏青淡蓝系渐变
            .background(pageGradientBrush(strong = false, theme = "home"))
    ) {
        HomeTopBar(onOpenSearch = onOpenSearch, onOpenMenu = onOpenAllCategories, onOpenNotifications = onOpenNotifications)

        // 分类行始终占位：分类接口回来之前先渲染「全部 + 骨架胶囊」，
        // 数据到了原位替换。之前是 categories 非空才渲染整行，导致首屏没有分类、
        // 数据回来后整行插入，把下面的课程列表整体顶下去。
        CategoryTabs(
            items = listOf(null to "全部") + uiState.categories,
            selectedId = uiState.selectedCategoryId,
            onSelect = viewModel::selectCategory,
            modifier = Modifier.padding(bottom = 4.dp),
            // 用随主题切换的 TabHomeInk：浅色藏青、深色亮蓝，避免深色下深紫黑文字看不见
            selectedColor = com.github.learningplatform.ui.theme.TabHomeInk,
            placeholderCount = if (uiState.categoriesLoaded) 0 else 2
        )

        when {
            uiState.isLoading && uiState.hotCourses.isEmpty() -> LoadingState()
            uiState.error != null && uiState.hotCourses.isEmpty() ->
                ErrorState(message = uiState.error.orEmpty(), onRetry = viewModel::refresh)
            else -> androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = uiState.isLoading && uiState.hotCourses.isNotEmpty(),
                onRefresh = viewModel::refresh
            ) {
                CourseFeed(
                    uiState = uiState,
                    onOpenAllCategories = onOpenAllCategories,
                    onOpenCourse = onOpenCourse,
                    onLoadMore = viewModel::loadMore
                )
            }
        }
    }

    // 右下角阿星入口：浮在列表之上、底栏之上方
    AxingFloatingEntry(
        onClick = onOpenAxing,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 10.dp)
    )
    }
}

@Composable
private fun HomeTopBar(onOpenSearch: () -> Unit, onOpenMenu: () -> Unit, onOpenNotifications: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenMenu) {
            Icon(NavIcons.Menu, contentDescription = "菜单", tint = TextPrimary)
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(50))
                // 浅色下 surface 即白色，深色下自动变深蓝灰，不写死 Color.White
                .background(MaterialTheme.colorScheme.surface)
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

        IconButton(onClick = onOpenNotifications) {
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
            .background(pageGradientBrush(strong = false, theme = "home")),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            // 优先用后端配置的轮播图（/api/v1/banner/list）；
            // 后台没配或请求失败时回落到「课程前 5 条」的旧展示，避免首页顶部空掉。
            if (uiState.banners.isNotEmpty()) {
                BannerCarousel(
                    banners = uiState.banners,
                    onOpenCourse = onOpenCourse,
                    // 水平留白交给 LazyRow 的 contentPadding 处理，才能左右对称
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                FeaturedCourseCarousel(
                    courses = uiState.featuredCourses,
                    onOpenCourse = onOpenCourse,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // 首页信息流原生广告位：有素材就显示卡片，无填充整块隐藏
        item {
            TakuNativeAdCard(
                placementId = BuildConfig.TAKU_HOME_NATIVE_PLACEMENT_ID,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // 排序筛选标签
        item {
            val sorts = CourseSort.entries
            var selectedSort by remember { mutableStateOf(CourseSort.SCORE) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sorts.forEach { s ->
                    val selected = s == selectedSort
                    Text(
                        text = s.label,
                        fontSize = 14.sp,
                        color = if (selected) TextPrimary else TextHint,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.clickable { selectedSort = s }
                    )
                }
            }
            // 排序后的课程列表 - 2列网格
            // 高分 → 评分降序；热门 → 点赞数降序；最新 → 创建时间降序（yyyy-MM-dd HH:mm:ss 可直接按字符串比较）
            val sortedCourses = when (selectedSort) {
                CourseSort.SCORE -> uiState.hotCourses.sortedByDescending { it.averageScore }
                CourseSort.HOT -> uiState.hotCourses.sortedByDescending { it.likeCount }
                CourseSort.NEWEST -> uiState.hotCourses.sortedByDescending { it.createTime }
            }
            if (sortedCourses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("该分类下暂无课程", color = TextHint, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                // 每两个一行
                sortedCourses.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { course ->
                            CourseCard(
                                course = course,
                                sort = selectedSort,
                                onOpenCourse = onOpenCourse,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        if (uiState.hasMore) {
            item {
                LaunchedEffect(Unit) { onLoadMore() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoadingMore) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
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

/** 首页课程排序方式：高分（评分）/ 热门（点赞数）/ 最新（创建时间） */
private enum class CourseSort(val label: String) {
    SCORE("高分"),
    HOT("热门"),
    NEWEST("最新")
}

@Composable
private fun CourseCard(
    course: CourseDto,
    sort: CourseSort,
    modifier: Modifier = Modifier,
    onOpenCourse: (Long) -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            // 浅色下 surface 即白色，深色下自动变深蓝灰
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable { onOpenCourse(course.courseId) }
    ) {
        Box {
            NetImage(
                url = course.coverUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                radius = 12.dp
            )
            Icon(
                NavIcons.Play,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(18.dp)
            )
        }
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = course.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            // 卡片副标题跟随当前 tab：高分看评分、热门看点赞数（用心形）、最新看创建日期
            val metric = when (sort) {
                CourseSort.SCORE -> String.format(Locale.CHINA, "%.1f分", course.averageScore)
                // 用「数量 + ❤️」代替「数量 + 赞」，更直观也不占字宽
                CourseSort.HOT -> formatCount(course.likeCount) + "❤️"
                CourseSort.NEWEST -> course.createTime.take(10).ifBlank { "—" }
            }
            // 观看人数：后端返回了才显示（null = 接口没这个字段，避免硬写「0 人观看」）
            val viewers = course.viewCount?.let { " · " + formatCount(it) + "人观看" } ?: ""
            Text(
                text = metric + viewers,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
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
    /*
     * 虚拟无限列表：item 数量给 Int.MAX_VALUE，起始位置对齐到 size 的整数倍。
     * 这样自动播放永远是「当前 +1」，不会出现「从最后一页动画滑回第一页」的长距离倒带
     * —— 那个回绕既慢，看起来也像卡住。
     */
    val virtualStart = remember(courses.size) {
        if (courses.size > 1) {
            val mid = Int.MAX_VALUE / 2
            mid - mid % courses.size
        } else 0
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = virtualStart)
    val scope = rememberCoroutineScope()
    var autoScrollSuspended by remember { mutableStateOf(false) }

    // firstVisibleItemIndex 是 @FrequentlyChangingValue（滚动时每一帧都变），
    // 直接读会在滚动期间触发整页重组；用 derivedStateOf 只在「页码变化」时才通知 UI。
    val currentPage by remember(courses.size) {
        derivedStateOf {
            if (courses.isEmpty()) 0 else listState.firstVisibleItemIndex % courses.size
        }
    }

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
            delay(5000)
            // 永远向前翻一页；虚拟列表没有边界，不存在回绕
            runCatching { listState.animateScrollToItem(listState.firstVisibleItemIndex + 1) }
        }
    }

    Column(modifier = modifier) {
        /*
         * 版式：每「页」宽度 = 视口宽，卡片在页内水平居中，占 92% 宽。
         * 左右各留 4% 作为视觉间隙，相邻两页的露出宽度因此恒等 ——
         * 旧版靠 contentPadding / spacedBy 做间距时，第一页贴左、后面的页宽度不一，
         * 翻页就会左右跳动，看起来像卡顿。
         *
         * 注意：LazyRow 给 item 的横向约束是无界的，item 里不能用 fillMaxWidth
         * （会退化成 wrap-content，卡片宽随图片原始尺寸变化），必须显式 width(maxWidth)。
         *
         * 卡片用 RoundedCornerShape 单独圆角，容器不裁剪，相邻卡片才能露出边缘。
         */
        val cardWidthRatio = 0.92f

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        ) {
            val cardWidth = maxWidth * cardWidthRatio
            // 每页占满视口宽、卡片在页内居中：snap 对齐整页后左右留白始终相等，
            // 不会出现「第一页贴左、翻页后又贴左」的间距跳变
            val sideInset = (maxWidth - cardWidth) / 2
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // 页与页之间不加额外间距，视觉间距由每页内部两侧留白组成
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
            ) {
                items(
                    count = if (courses.size > 1) Int.MAX_VALUE else 1,
                    key = { it }
                ) { index ->
                    val course = courses[index % courses.size]
                    Box(
                        // 页宽 = 视口宽，卡片靠左右 padding 居中；snap 以整页为单位
                        // LazyRow 给 item 的横向约束是「无界」，fillMaxWidth 会退化成
                        // wrap-content（卡片宽随图片原始尺寸变化，间距就不等）；
                        // 必须显式给页宽 = 视口宽，snap 才能按整页对齐
                        modifier = Modifier
                            .width(maxWidth)
                            .padding(horizontal = sideInset)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            // 图还没解出来之前先铺一层浅灰蓝，滑动时不会白闪
                            .background(Color(0xFFE7ECF5))
                            .clickable { onOpenCourse(course.courseId) }
                    ) {
                        NetImage(
                            url = course.coverUrl,
                            modifier = Modifier.fillMaxSize(),
                            radius = 0.dp
                        )

                        // 去掉黑色实心信息条：改成图片底部的渐变压暗。
                        // 文字依旧压得住，但视觉上不再是「图上切一块黑」。
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(104.dp)
                                .background(
                                    Brush.verticalGradient(
                                        0.00f to Color.Transparent,
                                        0.40f to Color.Black.copy(alpha = 0.42f),
                                        1.00f to Color.Black.copy(alpha = 0.90f)
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = course.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.55f),
                                            offset = Offset(0f, 2f),
                                            blurRadius = 8f
                                        )
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(3.dp))
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
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.45f),
                                            offset = Offset(0f, 1f),
                                            blurRadius = 6f
                                        )
                                    ),
                                    color = Color.White.copy(alpha = 0.80f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // 指示器移到文字行右侧，不再压在图中间
                            if (courses.size > 1) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, bottom = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    repeat(courses.size) { i ->
                                        val active = i == currentPage
                                        Box(
                                            modifier = Modifier
                                                .height(4.dp)
                                                .width(if (active) 14.dp else 5.dp)
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
            }

            // 指示器已经移进每张卡的文字行，这里不再需要覆盖层
        }
    }
}

/**
 * 首页轮播图（后端 banner 配置）。
 *
 * 与 [FeaturedCourseCarousel] 的区别：
 *  - 图片取 banner.imageUrl（不是课程封面），标题取 banner.title
 *  - 副标题用关联课程名 courseName
 *  - 点击按 courseId 跳课程；没有关联课程的 banner 点了不跳
 *  - 轮播顺序按后端 orderNum
 */
@Composable
private fun BannerCarousel(
    banners: List<BannerDto>,
    onOpenCourse: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return

    // 与课程轮播一致的虚拟无限列表：翻页永远向前，不回绕
    val virtualStart = remember(banners.size) {
        if (banners.size > 1) {
            val mid = Int.MAX_VALUE / 2
            mid - mid % banners.size
        } else 0
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = virtualStart)
    var autoScrollSuspended by remember { mutableStateOf(false) }

    val currentPage by remember(banners.size) {
        derivedStateOf {
            if (banners.isEmpty()) 0 else listState.firstVisibleItemIndex % banners.size
        }
    }

    // 用户手动滑动后暂停 8 秒自动轮播
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            autoScrollSuspended = true
        } else if (autoScrollSuspended) {
            delay(8000)
            autoScrollSuspended = false
        }
    }

    LaunchedEffect(banners.size, autoScrollSuspended) {
        if (banners.size <= 1 || autoScrollSuspended) return@LaunchedEffect
        while (true) {
            delay(5000)
            runCatching { listState.animateScrollToItem(listState.firstVisibleItemIndex + 1) }
        }
    }

    val cardWidthRatio = 0.92f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
    ) {
        val cardWidth = maxWidth * cardWidthRatio
        // 每页占满视口宽、卡片在页内居中，翻页后左右留白始终相等
        val sideInset = (maxWidth - cardWidth) / 2
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(
                count = if (banners.size > 1) Int.MAX_VALUE else 1,
                key = { it }
            ) { index ->
                val banner = banners[index % banners.size]
                Box(
                    // 页宽 = 视口宽，卡片靠左右 padding 居中；snap 以整页为单位
                    // 横向无界约束下 fillMaxWidth 无效，显式给页宽 = 视口宽
                    modifier = Modifier
                        .width(maxWidth)
                        .padding(horizontal = sideInset)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE7ECF5))
                        .clickable(enabled = banner.courseId > 0) {
                            onOpenCourse(banner.courseId)
                        }
                ) {
                    NetImage(
                        url = banner.imageUrl,
                        modifier = Modifier.fillMaxSize(),
                        radius = 0.dp
                    )

                    // 黑色实心条 → 图片底部渐变压暗，和课程轮播同一套语言
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(104.dp)
                            .background(
                                Brush.verticalGradient(
                                    0.00f to Color.Transparent,
                                    0.40f to Color.Black.copy(alpha = 0.42f),
                                    1.00f to Color.Black.copy(alpha = 0.90f)
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = banner.title.ifBlank { banner.courseName },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.55f),
                                        offset = Offset(0f, 2f),
                                        blurRadius = 8f
                                    )
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (banner.courseName.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = banner.courseName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.45f),
                                            offset = Offset(0f, 1f),
                                            blurRadius = 6f
                                        )
                                    ),
                                    color = Color.White.copy(alpha = 0.80f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (banners.size > 1) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, bottom = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                repeat(banners.size) { i ->
                                    val active = i == currentPage
                                    Box(
                                        modifier = Modifier
                                            .height(4.dp)
                                            .width(if (active) 14.dp else 5.dp)
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
        }

        // 指示器已移进卡片文字行，这里不再需要覆盖层
    }
}
