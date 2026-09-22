package com.github.learningplatform.ui.course

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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.learningplatform.data.remote.dto.ContentItemDto
import com.github.learningplatform.ui.theme.SoftBlueGradient
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.common.ErrorState
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.common.NetImage
import com.github.learningplatform.ui.common.SectionHeader
import com.github.learningplatform.ui.common.TagChip
import com.github.learningplatform.ui.common.formatCount
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Success
import com.github.learningplatform.ui.theme.Surface as AppSurface
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/**
 * 搜索页（接口文档 5.1 / 5.2）。
 *
 * 支持两种内容：type=1 文章、type=2 视频课程；
 * 结果项用 ContentItemDto（4.1 分类内容项与 5.1 搜索结果共用同一结构）。
 * 关键词为空时展示热词榜，点热词直接发起搜索。
 */
@Composable
fun SearchScreen(
    initialCategoryId: Long?,
    initialKeyword: String,
    onBack: () -> Unit,
    onOpenCourse: (Long) -> Unit,
    onOpenArticle: (Long) -> Unit,
    /** 初始搜索类型：1-文章 2-视频课程。从社区进来传 1，直接落到「文章」页 */
    initialType: Int = 2,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(initialCategoryId, initialKeyword, initialType) {
        viewModel.init(initialCategoryId, initialKeyword, initialType)
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = true))
    ) {
        // ---- 搜索栏 ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(NavIcons.ArrowBack, contentDescription = "返回", tint = TextPrimary)
            }

            /*
             * 用 BasicTextField + Row 自己排版，而不是 M3 的 TextField。
             *
             * 原因：M3 TextField 有内建的 contentPadding，一旦用 .height(48.dp) 压高度，
             * 它不会为压缩后的高度重算 padding，提示文字会被顶到输入框顶部
             * （实测输入框 y 40..136，文字在 y 96..128，明显偏上、没有垂直居中）。
             *
             * BasicTextField 没有这些内建留白，外层 Row 的 verticalAlignment = CenterVertically
             * 就能把文字和图标都摆正 —— 和首页搜索框同一套结构，两处观感一致。
             */
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SoftBlueGradient)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.keyword.isEmpty()) {
                        Text(
                            text = "您想学习什么？",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextHint,
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = uiState.keyword,
                        onValueChange = viewModel::onKeywordChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        cursorBrush = SolidColor(Primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboard?.hide()
                            viewModel.search()
                        }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                if (uiState.keyword.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        NavIcons.Close,
                        contentDescription = "清空",
                        tint = TextHint,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { viewModel.onKeywordChange("") }
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = "搜索",
                style = MaterialTheme.typography.bodyMedium,
                color = Primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    keyboard?.hide()
                    viewModel.search()
                }
            )
        }

        TabRow(
            selectedTabIndex = uiState.type - 1,
            containerColor = Color.Transparent,
            contentColor = Primary
        ) {
            // 顺序必须与接口 5.1 的 type 对齐：1-文章 2-视频课程
            listOf("文章", "视频课程").forEachIndexed { index, title ->
                Tab(
                    selected = uiState.type == index + 1,
                    onClick = { viewModel.switchType(index + 1) },
                    text = { Text(title) }
                )
            }
        }

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(
                message = uiState.error.orEmpty(),
                onRetry = viewModel::search
            )
            uiState.searched && uiState.results.isEmpty() ->
                EmptyState(message = "没有找到相关内容")
            uiState.searched -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageGradientBrush(strong = true)),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    SectionHeader(title = "共 ${uiState.total} 条结果")
                }
                // key 用下标而不是 "${'$'}{type}-${'$'}{id}"：4.2/5.1 只约定返回「分页列表」，
                // 没有定义 ContentItemDto 的字段名。若后端实际返回 courseId/articleId，
                // 所有元素 id 都会解析成 0，用 id 做 key 会直接抛重复 key 异常。
                itemsIndexed(uiState.results) { index, item ->
                    SearchResultRow(
                        item = item,
                        onClick = {
                            // id 解析不出来时不要跳转到 id=0 的详情页
                            if (item.id <= 0L) return@SearchResultRow
                            if (uiState.type == 1) onOpenArticle(item.id) else onOpenCourse(item.id)
                        }
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Divider)
                    )
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
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "点击加载更多",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Primary,
                                    modifier = Modifier.clickable { viewModel.loadMore() }
                                )
                            }
                        }
                    }
                }
            }
            else -> HotWords(
                words = uiState.hotWords,
                onPick = { word ->
                    viewModel.onKeywordChange(word)
                    keyboard?.hide()
                    viewModel.search()
                }
            )
        }
    }
}

@Composable
private fun HotWords(words: List<String>, onPick: (String) -> Unit) {
    if (words.isEmpty()) {
        EmptyState(message = "暂无热门搜索")
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        SectionHeader(title = "热门搜索")
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            words.forEachIndexed { index, word ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Transparent,
                    modifier = Modifier.clickable { onPick(word) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (index < 3) Primary else TextHint,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = word,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(item: ContentItemDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            NetImage(
                url = item.coverUrl,
                modifier = Modifier
                    .width(110.dp)
                    .height(66.dp),
                radius = 10.dp
            )
            // 类型角标：v3.1 增补要求 ContentItemDto 必须回传 type，
            // 列表里标出来可以避免用户点进去才发现是另一种内容
            Box(modifier = Modifier.padding(4.dp)) {
                TagChip(
                    text = if (item.isArticle) "文章" else "视频",
                    color = if (item.isArticle) Primary else Success
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildString {
                    if (item.authorName.isNotBlank()) {
                        append(item.authorName)
                        append(" · ")
                    }
                    append(formatCount(item.viewCount))
                    append(" 阅读 · ")
                    append(formatCount(item.likeCount))
                    append(" 赞")
                    if (item.duration > 0) {
                        append(" · ")
                        append(item.duration / 60)
                        append(" 分钟")
                    } else if (item.score > 0) {
                        append(" · ")
                        append(item.score)
                        append(" 分")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
