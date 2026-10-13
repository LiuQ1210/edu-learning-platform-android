package com.github.learningplatform.ui.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.theme.pageGradientBrush
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.Primary

data class NotificationItem(
    val id: Long,
    val title: String,
    val body: String,
    val time: String,
    val read: Boolean = false,
    val category: String = "like",
    val deepLink: String? = null
)

private val demoNotifications = listOf(
    // 点赞：社区文章被赞
    NotificationItem(1, "小明赞了你的文章", "「Kotlin 协程取消机制完全指南」收到 32 个赞，最近：小明 10 分钟前赞", "10 分钟前", false, "like", "article:5001"),
    NotificationItem(2, "小红赞了你的文章", "「Compose 重组优化实战」收到 18 个赞，最近：小红 1 小时前赞", "1 小时前", false, "like", "article:5002"),
    // 评论：社区文章/课程评论/视频评论被回复
    NotificationItem(3, "小李评论了你的文章", "「Kotlin 协程取消机制完全指南」：\"讲得太清楚了，收藏了\"", "10 分钟前", false, "comment", "article:5001?comment:88009"),
    NotificationItem(4, "小王回复了你的评论", "你在「Java 高并发编程」课程下的评论收到回复：\"这个方法我也试过\"", "2 小时前", false, "comment", "course:1003?lesson:4004"),
    NotificationItem(5, "小张回复了你的评论", "你在视频「Android 性能优化」下的评论收到回复：\"谢谢分享\"", "昨天 21:30", true, "comment", "video:6001?comment:99001"),
    // 关注：社区被关注
    NotificationItem(6, "小明关注了你", "小明 关注了你，现在你们可以互相私信讨论学习了", "3 天前", true, "follow", "user:3001"),
    NotificationItem(7, "小红关注了你", "小红 关注了你，已成为你的粉丝", "2 天前", true, "follow", "user:3002"),
    // 收藏：社区文章被收藏
    NotificationItem(8, "小王收藏了你的文章", "「Compose 重组优化实战」被小王收藏", "3 小时前", false, "favorite", "article:5002"),
    NotificationItem(9, "小张收藏了你的文章", "「Kotlin 协程取消机制完全指南」被小张收藏", "昨天 09:00", true, "favorite", "article:5001")
)

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenCourse: (Long, Long?) -> Unit,
    onOpenArticle: (Long, Long?) -> Unit,
    onOpenVideo: (Long?) -> Unit,
    onOpenUser: (Long) -> Unit,
    onOpenTodos: () -> Unit
) {
    val readIds = remember { mutableStateListOf<Long>() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("点赞", "评论", "关注", "收藏")
    val tabKeys = listOf("like", "comment", "follow", "favorite")

    val filtered = demoNotifications.filter { it.category == tabKeys[selectedTab] }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush())
    ) {
        BackTopBar(title = "通知", onBack = onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { i, label ->
                val selected = selectedTab == i
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) Primary else MaterialTheme.colorScheme.surface)
                        .clickable { selectedTab = i }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        color = if (selected) Color.White else TextSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered) { item ->
                val isRead = item.read || readIds.contains(item.id)
                NotificationRow(item = item.copy(read = isRead), onClick = {
                    readIds.add(item.id)
                    item.deepLink?.let { link ->
                        when {
                            link.startsWith("course:") -> {
                                val main = link.substringAfter(":")
                                val courseId = main.substringBefore("?").toLongOrNull() ?: return@let
                                val lessonId = main.substringAfter("lesson:", "").toLongOrNull()
                                onOpenCourse(courseId, lessonId)
                            }
                            link.startsWith("article:") -> {
                                val main = link.substringAfter(":")
                                val articleId = main.substringBefore("?").toLongOrNull() ?: return@let
                                val commentId = main.substringAfter("comment:", "").toLongOrNull()
                                onOpenArticle(articleId, commentId)
                            }
                            link.startsWith("todo:") -> onOpenTodos()
                            link.startsWith("video:") -> {
                                val vid = link.substringAfter(":").substringBefore("?").toLongOrNull()
                                val cid = link.substringAfter("comment:", "").toLongOrNull()
                                onOpenVideo(cid)
                            }
                            link.startsWith("user:") -> {
                                val uid = link.substringAfter(":").toLongOrNull() ?: return@let
                                onOpenUser(uid)
                            }
                        }
                    }
                })
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (item.read) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    else Primary
                )
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    fontWeight = if (item.read) FontWeight.Normal else FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(item.time, fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Text(item.body, fontSize = 13.sp, color = TextSecondary)
        }
    }
}
