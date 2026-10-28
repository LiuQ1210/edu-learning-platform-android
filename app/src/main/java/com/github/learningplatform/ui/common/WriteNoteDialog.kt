package com.github.learningplatform.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.learningplatform.ui.nav.NavIcons
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.TextHint
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.surfaceWashBrush

/**
 * 从内容页写笔记。
 *
 * 为什么必须带 `sourceType` / `sourceId`：
 * 接口 10.1 的契约要求笔记**必须归属于某个内容**，不支持独立笔记。
 * 所以这个弹窗只能从文章详情或播放页唤起，不能做成「新建笔记」的全局入口。
 *
 * @param sourceType 1-文章 2-视频/课程
 * @param sourceTitle 展示「来自：xxx」，让用户确认写在哪篇内容下
 * @param videoTimestamp 视频笔记带的播放位置（秒）。文章笔记传 null
 * @param saving 保存中，禁用按钮防重复提交
 * @param onSave 回传 (标题, 正文)
 */
@Composable
fun WriteNoteDialog(
    sourceType: Int,
    sourceTitle: String,
    videoTimestamp: Int? = null,
    saving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    // 正文为空就没必要保存 —— 笔记可以没标题，但不能没内容
    val canSave = content.isNotBlank() && !saving

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("写笔记") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 来源条：让用户确认笔记挂在哪个内容下
                if (sourceTitle.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(surfaceWashBrush())
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (sourceType == 2) NavIcons.Video else NavIcons.Community,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "来自：" + sourceTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // 视频笔记带时间戳，便于日后按位置回看
                if (videoTimestamp != null && videoTimestamp > 0) {
                    Text(
                        text = "笔记位置：" + formatTimestamp(videoTimestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHint
                    )
                    Spacer(Modifier.height(10.dp))
                }

                CommonTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "标题",
                    placeholder = "笔记标题（可留空）"
                )

                Spacer(Modifier.height(10.dp))

                CommonTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = "内容",
                    placeholder = "记下你的想法…"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, content) },
                enabled = canSave
            ) {
                Text(if (saving) "保存中…" else "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text("取消")
            }
        }
    )
}

/**
 * 秒数转 `mm:ss`（超过一小时转 `h:mm:ss`）。
 *
 * 笔记里的「视频位置」是给人看的，不是精确时间码，
 * 所以不用补零到固定宽度，短一点更易读。
 */
internal fun formatTimestamp(seconds: Int): String {
    if (seconds <= 0) return "00:00"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(java.util.Locale.ROOT, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(java.util.Locale.ROOT, "%02d:%02d", m, s)
    }
}
