package com.github.learningplatform.ui.common

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.github.learningplatform.data.remote.dto.ArticleDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.SurfaceWashStart
import com.github.learningplatform.ui.theme.surfaceWashBrush
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Success
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

// ---------------------------------------------------------------------------
// 基础原子
// ---------------------------------------------------------------------------

/**
 * 网络图片。
 *
 * Coil 3 的 AsyncImage 已内置跨平台 loader，OkHttp 网络层由
 * coil-network-okhttp 提供（见 build.gradle.kts）。
 *
 * 【任务⑤：图片按尺寸裁剪】布局完成后用 onSizeChanged 拿到容器真实像素尺寸，
 * 构造带 size 的 ImageRequest，让 Coil 按目标尺寸采样解码（inSampleSize），
 * 而不是整张原图解码后再缩小 —— 列表滚动时内存占用与解码开销显著下降。
 * 布局完成前尺寸为 0，此时按原尺寸加载，不阻塞首帧；布局一到位自动切换到裁剪尺寸。
 */
@Composable
fun NetImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    radius: Dp = 8.dp
) {
    var targetSize by remember { mutableStateOf(IntSize.Zero) }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .onSizeChanged { targetSize = it }
            .clip(RoundedCornerShape(radius))
            .background(Divider)
    ) {
        if (url.isNotBlank()) {
            val request = remember(url, targetSize) {
                ImageRequest.Builder(context)
                    .data(url)
                    .apply {
                        if (targetSize.width > 0 && targetSize.height > 0) {
                            size(targetSize.width, targetSize.height)
                        }
                    }
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** 圆形头像，url 为空时回退到首字母 */
@Composable
fun Avatar(
    url: String,
    name: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotBlank()) {
            // 【任务⑤：图片按尺寸裁剪】头像尺寸已知，直接按像素解码，避免整图解码。
            // LocalContext/LocalDensity 都是 @Composable 读取，须在 remember 外取值。
            val context = LocalContext.current
            val density = LocalDensity.current
            val request = remember(url, size) {
                ImageRequest.Builder(context)
                    .data(url)
                    .size(with(density) { size.toPx() }.toInt())
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name.take(1).ifBlank { "学" },
                color = Primary,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 2.4f).sp
            )
        }
    }
}

/** 统一返回栏 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(NavIcons.ArrowBack, contentDescription = "返回")
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = TextPrimary
        )
    )
}

/** 卡片式容器，统一圆角与留白 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

/** 区块标题（左侧粗体 + 右侧可选操作） */
@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        if (actionText != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                modifier = Modifier.clickable(enabled = onAction != null) { onAction?.invoke() }
            )
        }
    }
}

/** 次要信息文本 */
@Composable
fun MetaText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** 小徽标（分类名 / 状态） */
@Composable
fun TagChip(text: String, color: Color = Primary) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

/** 课程类型角标：短视频 / 长视频 */
@Composable
fun VideoTypeBadge(videoType: Int) {
    TagChip(
        text = if (videoType == 1) "短视频" else "长视频",
        color = if (videoType == 1) Primary else Success
    )
}

// ---------------------------------------------------------------------------
// 分类「胶囊」Tab（横向滚动，选中态描边 —— 对齐 UI 稿）
// ---------------------------------------------------------------------------

@Composable
fun CategoryTabs(
    /** id 为 null 表示「全部」；不要用 0 占位，否则会被当成真实分类 id 传给后端 */
    items: List<Pair<Long?, String>>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.first ?: -1L }) { (id, name) ->
            val selected = id == selectedId
            Surface(
                shape = RoundedCornerShape(50),
                color = if (selected) Primary.copy(alpha = 0.08f) else SurfaceWashStart,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) Primary else Divider
                ),
                // 再次点击已选中的分类不取消选择：「全部」是稳定的默认态
                modifier = Modifier.clickable { onSelect(id) }
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) Primary else TextSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 课程卡片：横版（列表）与竖版（推荐位）
// ---------------------------------------------------------------------------

/** 横版课程卡：左封面 + 右信息（对齐 UI 稿「热门课程」「热度排行」） */
@Composable
fun CourseRowCard(
    course: CourseDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            NetImage(
                url = course.coverUrl,
                modifier = Modifier
                    .width(120.dp)
                    .height(72.dp),
                radius = 10.dp
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = NavIcons.Play,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

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
            Spacer(Modifier.height(4.dp))
            MetaText(
                text = buildString {
                    if (course.studentCount > 0) append("${formatCount(course.studentCount)} 人正在学")
                    if (course.instructorName.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append("讲师：${course.instructorName}")
                    }
                }.ifBlank { "暂无学习数据" }
            )
        }

        trailing?.invoke()
    }
}

/** 竖版课程卡（推荐位，两列网格用） */
@Composable
fun CourseGridCard(
    course: CourseDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(surfaceWashBrush())
            .padding(bottom = 10.dp)
    ) {
        NetImage(
            url = course.coverUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f),
            radius = 12.dp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = course.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(Modifier.height(4.dp))
        MetaText(
            text = formatCount(course.studentCount) + " 人在学",
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// 文章卡片（社区流）
// ---------------------------------------------------------------------------

@Composable
fun ArticleRowCard(
    article: ArticleDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = article.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (article.summary.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(url = article.authorAvatar, name = article.authorName, size = 20.dp)
            Spacer(Modifier.width(6.dp))
            MetaText(article.authorName.ifBlank { "匿名" }, modifier = Modifier.weight(1f))
            MetaText("${formatCount(article.viewCount)} 阅读")
            Spacer(Modifier.width(10.dp))
            MetaText("${formatCount(article.likeCount)} 赞")
        }
    }
}

/** 120 万 / 1.2万 / 999 之类的中文计数 */
fun formatCount(value: Int): String = when {
    value >= 100_000_000 -> String.format("%.1f亿", value / 100_000_000.0)
    value >= 10_000 -> String.format("%.1f万", value / 10_000.0)
    else -> value.toString()
}
// ---------------------------------------------------------------------------
// 表单零件（登录/注册/编辑资料/笔记/待办共用）
// ---------------------------------------------------------------------------

/** 主按钮：整宽 + 最小 48dp 高，符合可点击区域规范 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * 通用输入框。
 *
 * @param visualTransformation 密码框传 PasswordVisualTransformation()，否则是明文
 * @param keyboardOptions      密码框传 KeyboardType.Password，手机号传 KeyboardType.Phone
 */
@Composable
fun CommonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            isError = isError,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        if (isError && errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
