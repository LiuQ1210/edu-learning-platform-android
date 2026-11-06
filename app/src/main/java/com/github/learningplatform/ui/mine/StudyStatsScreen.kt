package com.github.learningplatform.ui.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.learningplatform.ui.common.BackTopBar
import com.github.learningplatform.ui.common.EmptyState
import com.github.learningplatform.ui.theme.pageGradientBrush

/**
 * 学习数据/统计页。
 *
 * 后端还没有 `/user/study-stats`，之前的「今日 45 分钟 / 连续 7 天 / 本周曲线」
 * 全是写死的示例数字，已移除；接口就绪后再接真实数据。
 */
@Composable
fun StudyStatsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageGradientBrush(strong = false))
    ) {
        BackTopBar(title = "学习数据", onBack = onBack)
        EmptyState(
            message = "学习数据统计暂未接入后端",
            modifier = Modifier.padding(16.dp)
        )
    }
}
