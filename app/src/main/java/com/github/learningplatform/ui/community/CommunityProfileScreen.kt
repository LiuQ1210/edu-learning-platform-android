package com.github.learningplatform.ui.community

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.learningplatform.ui.common.Avatar
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.theme.pageGradientBrush
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

    // 进入页面时拉取资料与关注/粉丝数（自己走 /user/home，他人走 follow/count、follow/status）
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

    // 昵称/简介：自己来自 /user/home（与底部个人主页同源）；他人后端无资料接口，用占位文案
    val userName = if (isSelf) profileState.nickname.ifBlank { "未设置昵称" } else "小红"
    val bio = if (isSelf) profileState.bio else "热爱学习，正在补 Android 和算法。"
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    var userArticles by remember { mutableStateOf(listOf(
        Triple(5001L, "Kotlin 协程取消机制完全指南", "1.2k 阅读 · 32 赞"),
        Triple(5002L, "Compose 重组优化实战笔记", "860 阅读 · 18 赞"),
        Triple(5003L, "Android 性能优化从入门到实战", "2.1k 阅读 · 56 赞")
    )) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush())
    ) {
        BackTopBar(title = if (isSelf) "我的主页" else "$userName 的主页", onBack = onBack)

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
                        // 头像：自己用后端头像地址，空地址由 Avatar 用昵称首字母兜底；他人保持首字母占位
                        Avatar(
                            url = if (isSelf) profileState.avatar else "",
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
                                if (isSelf) profileState.sexLabel.ifBlank { "不公开" } else "男",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        // 右上角按钮：自己显示编辑，别人显示关注/取消关注
                        if (isSelf && onEdit != null) {
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
                        // 获赞：只有「我自己」能从 /user/home 拿到；他人后端未提供
                        StatColumn(
                            if (isSelf) profileState.likeReceivedCount.toString() else "—",
                            "获赞"
                        )
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

            // 粉丝头像墙（类似课程点赞）
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        listOf("张", "李", "王", "赵").forEachIndexed { i, initial ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(listOf(
                                        Color(0xFFFF8A80), Color(0xFF80D8FF),
                                        Color(0xFFB9F6CA), Color(0xFFFFD180)
                                    )[i]),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(initial, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                    Text(
                        "等 ${profileState.fansCount} 人关注",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            // TA 的文章
            item {
                Text(if (isSelf) "我的文章" else "TA 的文章", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            items(userArticles) { (id, title, meta) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onOpenArticle(id) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(meta, fontSize = 12.sp, color = TextSecondary)
                    }
                    if (isSelf) {
                        Text(
                            text = "编辑",
                            fontSize = 13.sp,
                            color = Primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onOpenArticle(id) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "删除",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { pendingDeleteId = id }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
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
                    userArticles = userArticles.filter { it.first != pendingDeleteId }
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
