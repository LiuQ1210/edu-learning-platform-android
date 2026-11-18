package com.github.learningplatform.ui.common

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.github.learningplatform.ui.theme.LocalAppDarkTheme
import com.github.learningplatform.ui.theme.TextHint

/**
 * 通用富文本 HTML 渲染（WebView）。
 *
 * 复用文章详情的方案：后端 summernote 编辑的正文是完整 HTML，手写 HTML -> AnnotatedString
 * 会丢段落 / 列表 / 加粗等格式，统一交给 WebView 渲染。
 *
 * 两个已处理的坑：
 *  1. WebView 在无界高度容器里会渲染成 ~0 高，这里通过 JS 回传真实内容高度钉住 Compose 高度。
 *  2. loadData 只在文档 / 主题变化时执行，避免重组反复重载导致闪烁、滚动位置丢失。
 *
 * 深色模式文字 / 背景随主题切换。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RichHtmlContent(html: String, modifier: Modifier = Modifier) {
    if (html.isBlank()) {
        Text(
            text = "暂无内容",
            style = MaterialTheme.typography.bodyMedium,
            color = TextHint,
            modifier = modifier
        )
        return
    }

    val dark = LocalAppDarkTheme.current
    val textColor = if (dark) "#E8EAF0" else "#1A1A1A"
    val bgColor = if (dark) "#1E2536" else "#FFFFFF"

    val document = remember(html, dark) {
        """
        <!DOCTYPE html>
        <html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
          body { margin:0; padding:0; background:$bgColor; color:$textColor;
                 font-size:16px; line-height:1.75; word-break:break-word;
                 font-family:-apple-system, "PingFang SC", "Microsoft YaHei", sans-serif; }
          p { margin:0 0 12px 0; }
          h1, h2, h3 { margin:14px 0 8px; }
          img, video { max-width:100%; height:auto; border-radius:8px; }
          table { width:100%; border-collapse:collapse; }
          td, th { border:1px solid #DDD; padding:6px; }
          blockquote { margin:0; padding-left:12px; border-left:3px solid #1A73E8; color:#666; }
          a { color:#1A73E8; text-decoration:none; }
        </style></head>
        <body>$html</body></html>
        """.trimIndent()
    }

    val density = LocalDensity.current
    var contentHeightDp by remember { mutableIntStateOf(240) }

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier.height(contentHeightDp.dp),
        factory = { context ->
            WebView(context).apply {
                // 只开 JS 用于内容高度回传，页面本身不执行任何业务脚本
                settings.javaScriptEnabled = true
                settings.defaultTextEncodingName = "UTF-8"
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    settings.forceDark = android.webkit.WebSettings.FORCE_DARK_AUTO
                }
                isVerticalScrollBarEnabled = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: android.webkit.WebResourceRequest
                    ): Boolean = true // 外链一律不接管，留在 App 内阅读

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        view.evaluateJavascript(
                            "(function(){return Math.ceil(document.body.scrollHeight);})()"
                        ) { value ->
                            val px = value?.trim()?.trim('"')?.toFloatOrNull() ?: return@evaluateJavascript
                            if (px > 0f) {
                                val dp = with(density) { px.toDp().value }.toInt()
                                if (dp > 0) contentHeightDp = dp.coerceAtLeast(120)
                            }
                        }
                    }
                }
                loadDataWithBaseURL(null, document, "text/html", "UTF-8", null)
                tag = document
            }
        },
        update = { webView ->
            if (webView.tag != document) {
                webView.tag = document
                webView.loadDataWithBaseURL(null, document, "text/html", "UTF-8", null)
            }
        }
    )
}
