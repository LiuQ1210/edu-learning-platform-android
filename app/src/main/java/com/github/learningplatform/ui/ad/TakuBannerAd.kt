package com.github.learningplatform.ui.ad

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.learningplatform.data.ad.TakuAds
import com.github.learningplatform.ui.theme.Divider
import com.github.learningplatform.ui.theme.TextHint

/**
 * Taku 横幅广告。
 *
 * 隐私授权前渲染占位外观，授权后（[TakuAds.initialized] 变 true）换成真实的 [ATBannerView]。
 * 离开组合时 destroy，避免横幅的自动刷新定时器泄漏。
 */
@Composable
fun TakuBannerAd(
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
) {
    if (!TakuAds.initialized) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(10.dp))
                .background(Divider.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "广告位 · 待授权",
                fontSize = 12.sp,
                color = TextHint,
            )
        }
        return
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        factory = { context -> TakuAds.createBannerView(context) },
        onRelease = { view -> view.destroy() },
    )
}
