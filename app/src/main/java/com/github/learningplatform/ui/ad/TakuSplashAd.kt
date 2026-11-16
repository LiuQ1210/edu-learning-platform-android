package com.github.learningplatform.ui.ad

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Typeface
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import com.github.learningplatform.R
import com.github.learningplatform.data.ad.TakuAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** 等待开屏加载完成的最长时间，超时直接进主界面 */
private const val AD_WAIT_TIMEOUT_MS = 2000L

/** 启动页最短展示时长，避免广告加载太快导致页面一闪而过 */
private const val SPLASH_MIN_MS = 900L

/** 等 SDK 初始化的最长时间（首次启动时用户可能还没同意隐私政策） */
private const val SDK_READY_TIMEOUT_MS = 8000L

/** 展示后兜底：SDK 迟迟不回调关闭时强制收起，避免卡在广告上 */
private const val AD_SHOW_SAFETY_MS = 8000L

/**
 * 开屏广告闸门。
 *
 * 顺序固定为「启动页 → 开屏广告 → 首页」：启动页画面出现的同时并行加载开屏，
 * 广告就绪并展示（或者加载失败/超时）后才放行主界面，用户不会看到先到首页、
 * 一秒后再弹广告。
 *
 * 运行在进程级作用域而不是组合作用域：Compose 重组/重建不会把等待中断，
 * 状态也只在这里持有一份。
 */
object SplashAdGate {

    /** 为 true 时主界面可以出现 */
    var passed by mutableStateOf(false)
        private set

    private var started = false
    private var hostActivity: Activity? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun start(activity: Activity, context: Context) {
        if (started) {
            // 页面重建（切主题/旋转）会再进一次：广告还挂在旧 Activity 上就放弃，
            // 否则旧窗口销毁后等不到关闭回调，启动页会一直挡住主界面
            if (!passed && hostActivity != null && hostActivity !== activity) {
                TakuAds.log("开屏宿主 Activity 已重建，结束本次开屏")
                TakuAds.releaseSplash()
                hostActivity = null
                passed = true
            }
            return
        }
        started = true
        scope.launch {
            if (!awaitSdkReady()) {
                TakuAds.log("开屏闸门：SDK 未就绪，直接放行")
                passed = true
                return@launch
            }

            val beganAt = SystemClock.uptimeMillis()
            val loaded = withTimeoutOrNull(AD_WAIT_TIMEOUT_MS) {
                TakuAds.loadSplash(context).await()
            } ?: false
            TakuAds.log("开屏闸门：loaded=$loaded 耗时=${SystemClock.uptimeMillis() - beganAt}ms")

            val remain = SPLASH_MIN_MS - (SystemClock.uptimeMillis() - beganAt)
            if (remain > 0) delay(remain)

            if (!loaded) {
                // 没等到广告就丢掉，避免稍后加载完成又盖到首页上
                TakuAds.releaseSplash()
                passed = true
                return@launch
            }
            present(activity)
        }
    }

    /** 拿不到 Activity 等异常路径：不放广告，直接放行 */
    fun skip() {
        passed = true
    }

    private suspend fun awaitSdkReady(): Boolean {
        val deadline = SystemClock.uptimeMillis() + SDK_READY_TIMEOUT_MS
        while (!TakuAds.initialized) {
            if (SystemClock.uptimeMillis() > deadline) return false
            delay(50)
        }
        return true
    }

    private fun present(activity: Activity) {
        hostActivity = activity
        val (root, adHost) = buildContainer(activity)
        activity.addContentView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        var finished = false
        fun finish() {
            if (finished) return
            finished = true
            (root.parent as? ViewGroup)?.removeView(root)
            passed = true
        }

        TakuAds.log("开屏容器已挂载，等待布局完成")
        // 广告区高度由布局决定（广告区 + 品牌条分屏），必须等量好尺寸再 show，
        // 否则 SDK 往 0 高度的容器里塞视图，画面不显示也不回调展示
        root.doOnLayout {
            TakuAds.log("开屏容器布局完成 adHost=${adHost.width}x${adHost.height}，调用 show")
            TakuAds.showSplash(activity, adHost) { finish() }
        }

        scope.launch {
            delay(AD_SHOW_SAFETY_MS)
            if (!finished) {
                TakuAds.log("开屏未回调关闭，强制收起")
                TakuAds.releaseSplash()
                finish()
            }
        }
    }

    /**
     * 广告区在上、品牌条在下：广告占满剩余高度，底部固定一条奶油底品牌栏，
     * 与 Compose 启动页的底部品牌行同款（图标 + 应用名）。
     */
    private fun buildContainer(activity: Activity): Pair<ViewGroup, ViewGroup> {
        val res = activity.resources
        val density = res.displayMetrics.density
        val cream = 0xFFFFFBF2.toInt()
        val barHeight = (64 * density).toInt()

        val adHost = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
            // 不铺底色：素材还在下载时透出下面的启动页，避免出现一块空白奶油底
        }

        val brandMark = ImageView(activity).apply {
            setImageResource(R.drawable.brand_mark)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams((26 * density).toInt(), (26 * density).toInt())
        }

        val brandName = TextView(activity).apply {
            text = res.getString(R.string.app_name)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF1A1A1A.toInt())
        }

        val brandRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(brandMark)
            addView(
                brandName,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { marginStart = (8 * density).toInt() },
            )
        }

        val brandBar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(cream)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, barHeight)
            addView(brandRow)
        }

        // 全面屏下广告区铺到屏幕底，品牌条要给手势条/导航栏留出高度
        ViewCompat.setOnApplyWindowInsetsListener(brandBar) { view, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.setPadding(0, 0, 0, bottom)
            (view.layoutParams as LinearLayout.LayoutParams).height = barHeight + bottom
            insets
        }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(adHost)
            addView(brandBar)
        }
        return root to adHost
    }
}

/**
 * 启动页同时承担开屏广告的等待窗口：广告就绪并展示后才放行主界面。
 * 返回 true 表示可以进入首页/登录页。
 */
@Composable
fun rememberSplashAdGate(): Boolean {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = context.findActivity()
        if (activity == null) {
            TakuAds.log("开屏闸门：拿不到 Activity，直接放行")
            SplashAdGate.skip()
        } else {
            SplashAdGate.start(activity, context)
        }
    }
    return SplashAdGate.passed
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
