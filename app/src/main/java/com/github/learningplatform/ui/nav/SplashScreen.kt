package com.github.learningplatform.ui.nav

import androidx.compose.foundation.background
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
import com.github.learningplatform.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.learningplatform.ui.ad.AdSlot
import com.github.learningplatform.ui.theme.BrandGradient
import com.github.learningplatform.ui.theme.GlowOrb
import com.github.learningplatform.ui.theme.GradientBackground
import com.github.learningplatform.ui.theme.Primary
import com.github.learningplatform.ui.theme.TextPrimary
import com.github.learningplatform.ui.theme.TextSecondary

/**
 * 启动页（对齐设计稿「App启动页」）。
 *
 * 设计稿是奶油渐变底 + 中央柔光圆 + 插画 + 品牌名的组合：上方大幅留白，
 * 品牌名在视觉中心略偏下，底部一行小字 slogan。这里用同款配色与版式复刻，
 * 插画位（学生伏案）用柔光圆 + 占位图形代替 —— 有正式插画素材后替换
 * [SplashIllustration] 即可，版式不用动。
 *
 * 最短展示时长与 MainActivity 的系统启动窗口一致（700ms），
 * 两者接力，中间不会出现空白帧。
 */
@Composable
fun SplashScreen(minDurationMs: Long = 700L) {
    // 保证最少展示时间，否则 DataStore 读得比一帧还快，启动页会一闪而过
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(minDurationMs)
    }

    GradientBackground(showGlow = false) {
        // 中央柔光圆：设计稿里那个大圆形，插画与品牌名都压在它上面
        GlowOrb(
            modifier = Modifier.align(Alignment.Center),
            diameter = 340.dp,
            alpha = 0.9f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SplashIllustration()

            Spacer(Modifier.height(28.dp))

            // 品牌名：设计稿是大号蓝字
            Text(
                text = "学习平台",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "提升技能、精进能力.",
                fontSize = 15.sp,
                color = TextSecondary
            )
        }

        // 开屏广告位（预留）
        AdSlot(slotId = "startup_splash", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))

        // 底部品牌行
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                // 品牌图，与桌面图标、登录/注册页同源
                Image(
                    painter = painterResource(R.drawable.brand_mark),
                    contentDescription = null,
                    // 用 Fit 而不是 Crop：这张图近似正方形，Crop 会把右上角的播放键小图标切掉
            contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "学习平台",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
    }
}

/**
 * 插画占位。
 *
 * 设计稿正稿是「学生伏案看视频 + 便利贴 + 书架」的矢量插画。这里用基础图形
 * 拼一个同色系的简化版，保证版式和配色可直接评审；拿到正式素材后，
 * 把整个函数体换成 `Image(painter = painterResource(R.drawable.splash_illustration))`
 * 即可，外部布局无需调整。
 */
@Composable
private fun SplashIllustration() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 便利贴：暖黄 + 珊瑚红 + 插画蓝，对应设计稿左上角三张纸
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NoteCard(com.github.learningplatform.ui.theme.AccentYellow, 30.dp, 22.dp)
            NoteCard(com.github.learningplatform.ui.theme.BrandBlueLight, 26.dp, 34.dp)
            NoteCard(com.github.learningplatform.ui.theme.AccentCoral, 30.dp, 26.dp)
        }

        Spacer(Modifier.height(22.dp))

        // 桌面 + 屏幕
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(86.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Primary.copy(alpha = 0.10f), Primary.copy(alpha = 0.20f))))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(96.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BrandGradient)
            )
        }

        Spacer(Modifier.height(8.dp))

        // 桌腿
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(com.github.learningplatform.ui.theme.Outline)
        )
    }
}

@Composable
private fun NoteCard(color: androidx.compose.ui.graphics.Color, w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(width = w, height = h)
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.85f))
    )
}
