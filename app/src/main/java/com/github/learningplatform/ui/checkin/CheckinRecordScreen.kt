package com.github.learningplatform.ui.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.github.learningplatform.ui.nav.NavIcons
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 签到记录页（preview 模式下展示规则说明） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckinRecordScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("签到规则") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(NavIcons.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RuleCard("每日签到", "每天可签到一次，连续签到积分递增：5/8/10/12/15/18/20 分，第 7 天后循环")
            RuleCard("断签重置", "中断一天后，连续签到天数重置为 1")
            RuleCard("积分兑换", "10 积分 = 免广告 30 分钟；30 积分 = 2 小时；100 积分 = 24 小时")
            RuleCard("免广告权益", "兑换后看课程视频、文章等内容不展示广告，到期自动恢复")
        }
    }
}

@Composable
private fun RuleCard(title: String, desc: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(desc, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
