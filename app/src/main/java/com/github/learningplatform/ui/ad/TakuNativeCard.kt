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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.anythink.core.api.ATAdInfo
import com.anythink.core.api.IATThirdPartyMaterial
import com.anythink.nativead.api.ATNativeAdView
import com.anythink.nativead.api.ATNativeEventListener
import com.anythink.nativead.api.ATNativeImageView
import com.anythink.nativead.api.ATNativePrepareInfo
import com.anythink.nativead.api.NativeAd
import com.github.learningplatform.data.ad.TakuAds

private const val CARD_IMAGE_HEIGHT_DP = 140

/**
 * 信息流里的原生广告卡片（首页轮播下方、社区列表内）。
 *
 * 每个页面自己请求、自己销毁，不与短视频流共用那条一次性库存；
 * 无填充或加载中时整块不占位。
 */
@Composable
fun TakuNativeAdCard(placementId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var ad by remember(placementId) { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(placementId) {
        val dm = context.resources.displayMetrics
        val request = TakuAds.requestNative(
            context = context,
            placementId = placementId,
            widthPx = dm.widthPixels,
            heightPx = (CARD_IMAGE_HEIGHT_DP * dm.density).toInt(),
        ) { loaded -> ad = loaded }

        onDispose {
            request.cancel()
            ad?.destory()
            ad = null
        }
    }

    val current = ad
    if (current != null) {
        NativeAdCardView(current, placementId, modifier)
    }
}

@Composable
private fun NativeAdCardView(ad: NativeAd, placementId: String, modifier: Modifier) {
    val context = LocalContext.current
    val adView = remember(ad) { createCardView(context, ad, placementId) }

    DisposableEffect(ad) {
        onDispose { (adView as? ATNativeAdView)?.destory() }
    }

    AndroidView(factory = { adView }, modifier = modifier.fillMaxWidth())
}

private fun createCardView(context: Context, ad: NativeAd, placementId: String): View {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).toInt()

    val material = ad.adMaterial as? IATThirdPartyMaterial

    val badge = TextView(context).apply {
        text = "广告"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
        setTextColor(0xFF8A94A6.toInt())
        setPadding(dp(6), dp(2), dp(6), dp(2))
        background = GradientDrawable().apply {
            cornerRadius = dp(4).toFloat()
            setColor(0xFFF2F4F7.toInt())
        }
    }

    val mainImage = ATNativeImageView(context).apply {
        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        material?.mainImageUrl?.takeIf { it.isNotBlank() }?.let { setImage(it) }
    }

    val title = TextView(context).apply {
        text = material?.title.orEmpty()
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setTextColor(0xFF1A1A1A.toInt())
        setTypeface(typeface, Typeface.BOLD)
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
    }

    val desc = TextView(context).apply {
        val text = material?.descriptionText.orEmpty()
        this.text = text
        visibility = if (text.isBlank()) View.GONE else View.VISIBLE
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(0xFF7A8699.toInt())
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }

    val cta = TextView(context).apply {
        text = material?.callToActionText?.takeIf { it.isNotBlank() } ?: "查看详情"
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setTextColor(0xFFFFFFFF.toInt())
        gravity = Gravity.CENTER
        setPadding(dp(18), dp(7), dp(18), dp(7))
        background = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(0xFF3677EF.toInt())
        }
    }

    val ctaRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.END
        addView(cta, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(10), dp(12), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(0xFFFFFFFF.toInt())
            setStroke(dp(1), 0xFFECECEC.toInt())
        }
        addView(badge, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(
            mainImage,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(CARD_IMAGE_HEIGHT_DP))
                .apply { topMargin = dp(8) },
        )
        addView(
            title,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(10) },
        )
        addView(
            desc,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(4) },
        )
        addView(
            ctaRow,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(10) },
        )
    }

    val adView = ATNativeAdView(context).apply {
        addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    ad.setNativeEventListener(object : ATNativeEventListener {
        override fun onAdImpressed(view: ATNativeAdView, adInfo: ATAdInfo?) {
            TakuAds.log("原生曝光[$placementId]")
        }

        override fun onAdClicked(view: ATNativeAdView, adInfo: ATAdInfo?) {
            TakuAds.log("原生被点击[$placementId]")
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
