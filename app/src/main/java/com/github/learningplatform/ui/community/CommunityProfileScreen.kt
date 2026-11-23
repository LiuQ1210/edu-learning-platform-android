package com.github.learningplatform.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.learningplatform.data.remote.dto.PostStatus
import com.github.learningplatform.ui.common.Avatar
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.LoadingState
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.Primary

/** 社区个人主页：查看其他用户的资料和发布内容 */
@Composable
fun CommunityProfileScreen(
    userId: Long,
    onBack: () -> Unit,
    onOpenArticle: (Long) -> Unit,
    isSelf: Boolean = false,
    onEdit: (() -> Unit)? = null,
    viewModel: CommunityProfileViewModel = hiltViewModel()
) {
    val profileState by viewModel.uiState.collectAsStateWithLifecycle()

    // 进入页面时拉取资料与文章（自己走 /user/home + /post/myList，他人走 /publicProfile + /post/list）
    LaunchedEffect(userId, isSelf) {
        viewModel.load(userId, isSelf)
    }

    // 从编辑主页返回时（页面回到前台 ON_RESUME）重新拉一次，保证保存后的昵称/简介/计数即时刷新
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, userId, isSelf) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load(userId, isSelf)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 渲染语义以后端返回的 isSelf 为准（导航入参 self 只决定首次拉哪个接口）
    val self = profileState.isSelf

    // 昵称/简介：自己来自 /user/home，他人来自 /publicProfile；昵称仍为空时用「用户 id」兜底而不编造人名
    val userName = profileState.nickname.ifBlank {
        if (self) "未设置昵称" else "用户 $userId"
    }
    val bio = profileState.bio
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush())
    ) {
        BackTopBar(title = if (self) "我的主页" else "$userName 的主页", onBack = onBack)

        if (profileState.isLoading) {
            LoadingState()
        } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 头像 + 昵称 + 简介（左右布局）
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 头像：自己/他人都用后端头像地址，空地址由 Avatar 用昵称首字母兜底
                        Avatar(
                            url = profileState.avatar,
                            name = userName,
                            size = 56.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            if (bio.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(bio, fontSize = 12.sp, color = TextSecondary)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                profileState.sexLabel.ifBlank { "不公开" },
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        // 右上角按钮：自己显示编辑，别人显示关注/取消关注
                        if (self && onEdit != null) {
                            OutlinedButton(
                                onClick = onEdit,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("编辑", color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            val followed = profileState.isFollowed
                            OutlinedButton(
                                onClick = { viewModel.toggleFollow(userId) },
                                enabled = !profileState.submitting && !profileState.isLoading,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    when {
                                        profileState.submitting -> if (followed) "取消中..." else "关注中..."
                                        followed -> "已关注"
                                        else -> "关注"
                                    },
                                    color = if (followed) TextSecondary else Primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                        // 关注/粉丝/获赞 数据（小一点，横排）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatColumn(profileState.followCount.toString(), "关注")
                            StatColumn(profileState.fansCount.toString(), "粉丝")
                            StatColumn(profileState.likeReceivedCount.toString(), "获赞")
                        }

                    // 失败/提示信息（后端返回的业务文案）
                    val tip = profileState.error ?: profileState.message
                    if (!tip.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = tip,
                            fontSize = 12.sp,
                            color = if (profileState.error != null) MaterialTheme.colorScheme.error else TextSecondary
                        )
                    }
                }
            }

            // 粉丝数：后端只返回计数，没有「粉丝头像列表」接口
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("粉丝", fontSize = 13.sp, color = TextSecondary)
                    Text(
                        "${profileState.fansCount} 人",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            // 文章：自己来自 /post/myList；他人来自 /post/list?userId=
            item {
                Text(if (self) "我的文章" else "TA 的文章", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            if (profileState.articles.isEmpty()) {
                item {
                    Text(
                        text = if (self) "还没有发布文章" else "暂无文章",
                        fontSize = 13.sp,
                        color = TextHint,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(20.dp)
                    )
                }
            }
            items(profileState.articles, key = { it.articleId }) { article ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onOpenArticle(article.articleId) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = article.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 2,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (article.status != PostStatus.PUBLISHED) {
                                Spacer(Modifier.width(6.dp))
                                ArticleStatusBadge(article.status)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        // 阅读 / 点赞 / 发布时间全部来自后端字段
                        val meta = buildList {
                            add("${article.viewCount} 阅读")
                            add("${article.likeCount} 赞")
                            if (article.publishTime.isNotBlank()) add(article.publishTime.take(10))
                        }.joinToString(" · ")
                        Text(meta, fontSize = 12.sp, color = TextSecondary)
                    }
                    if (self) {
                        // 后端只有 add / list / myList / info / remove，没有 post/edit，
                        // 所以这里只留「删除」，编辑入口不假装可用
                        Text(
                            text = "删除",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { pendingDeleteId = article.articleId }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        }
    }

    if (pendingDeleteId != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("删除文章") },
            text = { Text("确定要彻底删除这篇文章吗？删除后不可恢复。") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    pendingDeleteId?.let { viewModel.deleteArticle(it) }
                    pendingDeleteId = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDeleteId = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

/** 文章状态徽章：审核中/已下架/草稿，只在非「已发布」时出现 */
@Composable
private fun ArticleStatusBadge(status: Int) {
    val (label, color) = when (status) {
        PostStatus.PENDING -> "审核中" to androidx.compose.ui.graphics.Color(0xFFE08A1E)
        PostStatus.REJECTED -> "未通过" to MaterialTheme.colorScheme.error
        PostStatus.OFFLINE -> "已下架" to TextSecondary
        else -> "草稿" to TextSecondary
    }
    Text(
        text = label,
        fontSize = 11.sp,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}
