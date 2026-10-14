package com.github.learningplatform.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.TextHint

/**
 * 【广告位预留】通用广告位容器。
 *
 * 暂不接广告 SDK，当前渲染一个**轻量占位外观**（浅灰圆角框 + slotCode 标签），
 * 用于在开发/预览阶段直观确认 4 个广告位的位置与展示时机；
 * 接入 Taku 时把 [Box] 内部换成真实广告 View 即可，上层布局不用动。
 *
 * 已插入的 4 个预留位：
 *  - 开屏：SplashScreen（startup_splash）
 *  - 首页信息流：HomeScreen（有广告位配置时渲染）
 *  - 播放页暂停：PlayerScreen（暂停时显示）
 *  - 社区列表：CommunityScreen（community_home_banner）
 *
 * @param slotCode 广告位代码（占位标签显示用）
 * @param height 预留区域高度；null 表示不占额外空间（信息流内由周围内容自然撑开）
 */
@Composable
fun AdSlotPlaceholder(
    slotCode: String? = null,
    height: Dp? = 64.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (height != null) Modifier.height(height) else Modifier)
            .clip(RoundedCornerShape(10.dp))
            .background(Divider.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        // 预留：Taku 广告容器挂载点（slotCode = ${slotCode.orEmpty()}）
        // 接入后整个 Box 内部替换为真实广告 View
        Text(
            text = "广告位 · ${slotCode ?: "未配置"}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextHint
        )
    }
}
