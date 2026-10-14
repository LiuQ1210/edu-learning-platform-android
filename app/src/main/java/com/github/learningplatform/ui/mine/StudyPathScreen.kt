package com.github.learningplatform.ui.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.Surface
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary
import com.github.learningplatform.ui.theme.pageGradientBrush

/**
 * 学习路径页（10-13 B）。
 * 按知识点串联课程的学习地图，现在是占位框架。
 */
@Composable
fun StudyPathScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = false))
    ) {
        BackTopBar(title = "学习路径", onBack = onBack)

        val paths = listOf(
            StudyPath("Android 开发入门", "从 Kotlin 到 Jetpack Compose，6 门课", "进行中"),
            StudyPath("后端开发必学", "Java → Spring Boot → MySQL，8 门课", "未开始"),
            StudyPath("数据分析实战", "SQL → Python → 可视化，5 门课", "已完成"),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "按学习路径系统学习，比单门课更高效",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
            }
            items(paths) { path ->
                PathCard(path = path)
            }
        }
    }
}

private data class StudyPath(
    val title: String,
    val desc: String,
    val status: String,
)

@Composable
private fun PathCard(path: StudyPath) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(path.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(path.desc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(10.dp))
            Text(
                when (path.status) {
                    "进行中" -> "继续学习"
                    "已完成" -> "已完成"
                    else -> "开始学习"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
