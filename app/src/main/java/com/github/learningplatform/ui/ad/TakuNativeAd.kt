package com.github.learningplatform.ui.ad

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anythink.core.api.ATAdInfo
import com.anythink.core.api.IATThirdPartyMaterial
import com.anythink.nativead.api.ATNativeAdView
import com.anythink.nativead.api.ATNativeEventListener
import com.anythink.nativead.api.ATNativeImageView
import com.anythink.nativead.api.ATNativePrepareInfo
import com.anythink.nativead.api.NativeAd
import com.github.learningplatform.data.ad.TakuAds

/**
 * 短视频流里的原生广告页（自渲染）。
 *
 * 素材里只有一张横版图和标题/描述/CTA，塞满竖屏会放大得很糊，所以做成「广告卡」：
 * 深色底上居中放图 + 文案 + 按钮，视觉上仍是完整的一屏。
 *
 * 曝光与点击由 SDK 通过 [NativeAd.renderAdContainer] + [NativeAd.prepare] 绑定，
 * 日志统一走 TakuTest 标签。
 */
@Composable
fun TakuNativeAdPage(ad: NativeAd, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adView = remember(ad) { createNativeAdView(context, ad) }

    DisposableEffect(ad) {
        onDispose {
            (adView as? ATNativeAdView)?.destory()
            ad.destory()
            TakuAds.log("原生广告页销毁")
        }
    }

    AndroidView(factory = { adView }, modifier = modifier.fillMaxSize())
}

/** 没有库存时的占位页，避免滚到一个纯黑屏 */
@Composable
fun TakuNativeAdPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "广告加载中…", color = Color(0xFF7A8699), fontSize = 14.sp)
    }
}

private fun createNativeAdView(context: Context, ad: NativeAd): View {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).toInt()

    val material = ad.adMaterial as? IATThirdPartyMaterial

    val badge = TextView(context).apply {
        text = "广告"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setTextColor(0xB3FFFFFF.toInt())
        setPadding(dp(8), dp(3), dp(8), dp(3))
        background = GradientDrawable().apply {
            cornerRadius = dp(4).toFloat()
            setColor(0x33FFFFFF)
        }
    }

    val mainImage = ATNativeImageView(context).apply {
        adjustViewBounds = true
        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        material?.mainImageUrl?.takeIf { it.isNotBlank() }?.let { setImage(it) }
    }

    val title = TextView(context).apply {
        text = material?.title.orEmpty()
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
        setTextColor(0xFFFFFFFF.toInt())
        setTypeface(typeface, Typeface.BOLD)
    }

    val desc = TextView(context).apply {
        val text = material?.descriptionText.orEmpty()
        this.text = text
        visibility = if (text.isBlank()) View.GONE else View.VISIBLE
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setTextColor(0xFFB8C2D9.toInt())
    }

    val cta = TextView(context).apply {
        text = material?.callToActionText?.takeIf { it.isNotBlank() } ?: "查看详情"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setTextColor(0xFFFFFFFF.toInt())
        gravity = Gravity.CENTER
        setPadding(dp(32), dp(11), dp(32), dp(11))
        background = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(0xFF3677EF.toInt())
        }
    }

    val hint = TextView(context).apply {
        text = "上滑继续观看视频"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(0xFF7A8699.toInt())
    }

    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(20), dp(56), dp(20), dp(40))
        setBackgroundColor(0xFF0E1526.toInt())
        addView(badge, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(
            mainImage,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(24) },
        )
        addView(
            title,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(20) },
        )
        addView(
            desc,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(8) },
        )
        addView(cta, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(22) })
        addView(
            hint,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(32) },
        )
    }

    val adView = ATNativeAdView(context).apply {
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    ad.setNativeEventListener(object : ATNativeEventListener {
        override fun onAdImpressed(view: ATNativeAdView, adInfo: ATAdInfo?) {
            TakuAds.log("原生曝光")
        }

        override fun onAdClicked(view: ATNativeAdView, adInfo: ATAdInfo?) {
            TakuAds.log("原生被点击")
        }

        override fun onAdVideoStart(view: ATNativeAdView) = Unit

        override fun onAdVideoEnd(view: ATNativeAdView) = Unit

        override fun onAdVideoProgress(view: ATNativeAdView, progress: Int) = Unit
    })

    // 先渲染（把素材挂进容器），再 prepare（绑定可点击区域/曝光统计）
    ad.renderAdContainer(adView, root)

    val prepareInfo = ATNativePrepareInfo().apply {
        parentView = root
        titleView = title
        descView = desc
        mainImageView = mainImage
        ctaView = cta
    }
    ad.prepare(adView, prepareInfo)

    return adView
}
