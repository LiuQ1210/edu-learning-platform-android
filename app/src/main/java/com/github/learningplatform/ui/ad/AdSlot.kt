package com.github.learningplatform.ui.ad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 广告位占位组件。
 *
 * slotId 与 DemoData.adSlots / 后端 AdSlotDto 对齐：
 *   startup_splash      开屏
 *   feed_home_banner    首页信息流第5条后
 *   video_pause_banner  播放页暂停位
 *   community_banner    社区列表第3条后
 *
 * 后期接 Taku 聚合 SDK 时只改这个文件内部实现。
 */
@Composable
fun AdSlot(
    slotId: String,
    modifier: Modifier = Modifier,
) {
    val h = when (slotId) {
        "startup_splash" -> 300.dp
        "feed_home_banner", "community_banner" -> 60.dp
        "video_pause_banner" -> 50.dp
        else -> 50.dp
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(h)
    )
}
