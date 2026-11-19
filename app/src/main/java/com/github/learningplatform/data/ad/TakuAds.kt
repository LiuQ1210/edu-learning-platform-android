package com.github.learningplatform.data.ad

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.anythink.banner.api.ATBannerListener
import com.anythink.banner.api.ATBannerView
import com.anythink.core.api.ATAdConst
import com.anythink.core.api.ATAdInfo
import com.anythink.core.api.ATSDK
import com.anythink.core.api.ATUserDeviceInfo
import com.anythink.core.api.AdError
import com.anythink.core.api.IATThirdPartyMaterial
import com.anythink.nativead.api.ATNative
import com.anythink.nativead.api.ATNativeNetworkListener
import com.anythink.nativead.api.NativeAd
import com.anythink.rewardvideo.api.ATRewardVideoAd
import com.anythink.rewardvideo.api.ATRewardVideoListener
import com.anythink.splashad.api.ATSplashAd
import com.anythink.splashad.api.ATSplashAdExtraInfo
import com.anythink.splashad.api.ATSplashAdListener
import com.github.learningplatform.BuildConfig
import com.github.learningplatform.R
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Taku 聚合广告（当前只有酷盈 ADX）的接入点。
 *
 * 所有回调节点统一打 `TakuTest` 标签的日志，调试阶段直接看 Logcat 验证链路。
 */
object TakuAds {
    private const val TAG = "TakuTest"

    /** 服务端广告领分要求至少观看 15 秒，回调没带够时长时按这个值兜底 */
    private const val MIN_REWARD_SECONDS = 15

    /**
     * 供 UI 感知初始化状态：横幅要等隐私同意、SDK 初始化完成后才能请求。
     * 用 Compose 状态承载，同意后社区页会自动挂上横幅。
     */
    var initialized by mutableStateOf(false)
        private set

    /** 原生广告是否已有可消费的库存，短视频流的广告页据此决定渲染还是占位 */
    var nativeReady by mutableStateOf(false)
        private set

    /** 最近一次原生请求是否以无填充/失败告终，广告页据此跳过占位页 */
    var nativeRequestFailed by mutableStateOf(false)
        private set

    /** 是否配置了激励视频广告位；未配置时不插入广告入口页 */
    val rewardConfigured: Boolean = BuildConfig.TAKU_REWARD_PLACEMENT_ID.isNotBlank()

    /** 激励视频是否已加载就绪，入口页据此启用「观看广告」按钮 */
    var rewardReady by mutableStateOf(false)
        private set

    var rewardLoading by mutableStateOf(false)
        private set

    var rewardFailed by mutableStateOf(false)
        private set

    private var rewardAd: ATRewardVideoAd? = null
    private var rewardContinuation: CancellableContinuation<RewardOutcome>? = null
    private var rewardShowId: String? = null
    private var rewardPlayStartAt = 0L

    /** 一次激励视频播放的结果；只有 [Completed] 可以领积分 */
    sealed interface RewardOutcome {
        data class Completed(val showId: String, val seconds: Int) : RewardOutcome

        /** 没看到发奖就关闭，不发积分 */
        data object ClosedEarly : RewardOutcome

        /** 广告还没加载好，调用方应提示稍后再试 */
        data object NotReady : RewardOutcome

        data object ShowFailed : RewardOutcome

        data object NotConfigured : RewardOutcome
    }

    private var appContext: Context? = null

    private var splashAd: ATSplashAd? = null

    /** 本次开屏加载的结果：true=可展示，false=无填充/超时。重复调用复用同一个实例 */
    private var splashResult: CompletableDeferred<Boolean>? = null

    private var splashOnFinished: (() -> Unit)? = null

    fun init(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        this.appContext = appContext
        ATSDK.init(appContext, BuildConfig.TAKU_APP_ID, BuildConfig.TAKU_APP_KEY)
        ATSDK.start()
        initialized = true
        log("SDK 初始化完成，版本 ${ATSDK.getSDKVersionName()}")

        if (BuildConfig.DEBUG) {
            ATSDK.setNetworkLogDebug(true)
            provideFallbackOaid(appContext)
        }

        // 原生信息流（短视频流内插入）：提前预热一条，滑到广告页时直接取用
        val dm = appContext.resources.displayMetrics
        loadNative(appContext, dm.widthPixels, dm.heightPixels)

        // 激励视频：任务中心和短视频广告入口都会用，提前加载减少点击后的等待
        ensureRewardLoaded(appContext)
    }

    /**
     * 模拟器没有 OAID，Taku ADX 的 S2S HeadBidding 会以 "Invalid Android OAID" 拒单，
     * 这里用 AndroidID 派生一个稳定伪值兜底。真机自带 OAID，不覆盖。
     */
    private fun provideFallbackOaid(context: Context) {
        if (!isEmulator()) return
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        )
        if (androidId.isNullOrEmpty()) return
        val fakeOaid = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
        ATSDK.setATUserDeviceInfo(ATUserDeviceInfo().apply { devOaid = fakeOaid })
        log("检测到模拟器，注入兜底 OAID=$fakeOaid")
    }

    private fun isEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu")

    fun createBannerView(context: Context): ATBannerView = ATBannerView(context).apply {
        setPlacementId(BuildConfig.TAKU_PLACEMENT_ID)
        setBannerAdListener(object : ATBannerListener {
            override fun onBannerLoaded() {
                log("横幅加载成功")
            }

            override fun onBannerFailed(error: AdError?) {
                log("横幅加载失败 code=${error?.code} desc=${error?.desc}")
            }

            override fun onBannerClicked(adInfo: ATAdInfo?) {
                log("横幅被点击")
            }

            override fun onBannerShow(adInfo: ATAdInfo?) {
                log("横幅展示")
            }

            override fun onBannerClose(adInfo: ATAdInfo?) {
                log("横幅关闭")
            }

            override fun onBannerAutoRefreshed(adInfo: ATAdInfo?) {
                log("横幅自动刷新")
            }

            override fun onBannerAutoRefreshFail(error: AdError?) {
                log("横幅自动刷新失败 code=${error?.code}")
            }
        })
        loadAd()
        log("已请求加载横幅 placement=${BuildConfig.TAKU_PLACEMENT_ID}")
    }

    private var nativeAd: NativeAd? = null
    private var nativeLoading = false
    private var nativeSize: Pair<Int, Int>? = null

    /**
     * 原生/信息流广告（短视频流里的插入页）。
     *
     * 传入广告页的展示尺寸供广告源挑选素材；加载成功后由短视频流取用。
     */
    fun loadNative(context: Context, widthPx: Int, heightPx: Int) {
        nativeSize = widthPx to heightPx
        if (BuildConfig.TAKU_NATIVE_PLACEMENT_ID.isBlank()) {
            log("未配置原生广告位，跳过")
            return
        }
        if (nativeAd != null || nativeLoading) return
        nativeLoading = true
        nativeRequestFailed = false

        val appContext = context.applicationContext
        var atNative: ATNative? = null
        atNative = ATNative(
            appContext,
            BuildConfig.TAKU_NATIVE_PLACEMENT_ID,
            object : ATNativeNetworkListener {
                override fun onNativeAdLoaded() {
                    nativeLoading = false
                    val ad = atNative?.getNativeAd()
                    nativeAd = ad
                    nativeReady = ad != null
                    if (ad == null) {
                        log("原生加载成功但广告对象为空")
                        return
                    }
                    val material = ad.adMaterial as? IATThirdPartyMaterial
                    log(
                        "原生加载成功 nativeType=${ad.nativeType} express=${ad.isNativeExpress()} " +
                            "title=${material?.title} cta=${material?.callToActionText} " +
                            "image=${material?.mainImageUrl} video=${material?.videoUrl} " +
                            "size=${material?.mainImageWidth}x${material?.mainImageHeight}"
                    )
                }

                override fun onNativeAdLoadFail(error: AdError?) {
                    nativeLoading = false
                    nativeReady = false
                    nativeRequestFailed = true
                    log("原生加载失败 code=${error?.code} desc=${error?.desc}")
                }
            },
        )
        atNative.setLocalExtra(
            mapOf(
                ATAdConst.KEY.AD_WIDTH to widthPx,
                ATAdConst.KEY.AD_HEIGHT to heightPx,
            ),
        )
        atNative.makeAdRequest()
        log("已请求加载原生 placement=${BuildConfig.TAKU_NATIVE_PLACEMENT_ID} size=${widthPx}x$heightPx")
    }

    /** 没有库存也没在加载时补一次请求（短视频滑到广告页时调用） */
    fun requestNativeIfNeeded(context: Context) {
        if (nativeAd != null || nativeLoading) return
        val size = nativeSize
            ?: context.resources.displayMetrics.let { it.widthPixels to it.heightPixels }
        loadNative(context, size.first, size.second)
    }

    /**
     * 取走当前库存并立刻补下一条。
     *
     * 原生广告是一条一次性的：展示过就必须换新实例，否则第二次展示不会上报曝光。
     */
    fun consumeNativeAd(): NativeAd? {
        val ad = nativeAd ?: return null
        nativeAd = null
        nativeReady = false
        val size = nativeSize
        appContext?.let { ctx -> if (size != null) loadNative(ctx, size.first, size.second) }
        return ad
    }

    /**
     * 页面自持的一次性原生请求（首页/社区信息流用）。
     *
     * 一条原生广告只能挂在一个视图上，所以这些页面各自请求、各自销毁，
     * 不共用短视频流那份单条缓存。
     */
    class NativeAdRequest internal constructor() {
        private var cancelled = false
        internal var onResult: ((NativeAd?) -> Unit)? = null

        /** 页面销毁时调用：之后的回调不再分发 */
        fun cancel() {
            cancelled = true
            onResult = null
        }

        internal fun deliver(ad: NativeAd?) {
            if (!cancelled) onResult?.invoke(ad)
        }
    }

    /** 发起一次独立原生请求；成功回调广告对象，无填充回调 null */
    fun requestNative(
        context: Context,
        placementId: String,
        widthPx: Int,
        heightPx: Int,
        onResult: (NativeAd?) -> Unit,
    ): NativeAdRequest {
        val request = NativeAdRequest()
        request.onResult = onResult
        if (placementId.isBlank()) {
            log("未配置原生广告位，跳过")
            request.deliver(null)
            return request
        }

        val appContext = context.applicationContext
        var atNative: ATNative? = null
        atNative = ATNative(
            appContext,
            placementId,
            object : ATNativeNetworkListener {
                override fun onNativeAdLoaded() {
                    val ad = atNative?.getNativeAd()
                    if (ad == null) log("原生加载成功但广告对象为空[$placementId]")
                    request.deliver(ad)
                }

                override fun onNativeAdLoadFail(error: AdError?) {
                    log("原生加载失败[$placementId] code=${error?.code} desc=${error?.desc}")
                    request.deliver(null)
                }
            },
        )
        atNative.setLocalExtra(
            mapOf(
                ATAdConst.KEY.AD_WIDTH to widthPx,
                ATAdConst.KEY.AD_HEIGHT to heightPx,
            ),
        )
        atNative.makeAdRequest()
        log("已请求加载原生[$placementId] size=${widthPx}x$heightPx")
        return request
    }

    // ------------------------------------------------------------------ 激励视频

    /**
     * 预加载激励视频；加载中或已有库存时直接返回。
     *
     * 激励视频必须由用户点击触发，这里只负责备好库存，不会自动弹出。
     */
    fun ensureRewardLoaded(context: Context) {
        if (!rewardConfigured) return
        if (rewardAd != null || rewardLoading) return

        val appContext = context.applicationContext
        rewardLoading = true
        rewardFailed = false

        val ad = ATRewardVideoAd(appContext, BuildConfig.TAKU_REWARD_PLACEMENT_ID)
        ad.setAdListener(object : ATRewardVideoListener {
            override fun onRewardedVideoAdLoaded() {
                if (rewardAd !== ad) return
                rewardLoading = false
                rewardReady = true
                rewardFailed = false
                log("激励视频加载成功")
            }

            override fun onRewardedVideoAdFailed(error: AdError?) {
                if (rewardAd !== ad) return
                rewardLoading = false
                rewardReady = false
                rewardFailed = true
                rewardAd = null
                ad.destroyAd()
                log("激励视频加载失败 code=${error?.code} desc=${error?.desc}")
            }

            override fun onRewardedVideoAdPlayStart(adInfo: ATAdInfo?) {
                if (rewardAd !== ad) return
                rewardPlayStartAt = SystemClock.elapsedRealtime()
                log("激励视频开始播放")
            }

            override fun onRewardedVideoAdPlayEnd(adInfo: ATAdInfo?) {
                if (rewardAd !== ad) return
                log("激励视频播放结束")
            }

            override fun onRewardedVideoAdPlayFailed(error: AdError?, adInfo: ATAdInfo?) {
                if (rewardAd !== ad) return
                log("激励视频播放失败 code=${error?.code} desc=${error?.desc}")
                finishReward(RewardOutcome.ShowFailed)
            }

            override fun onRewardedVideoAdClosed(adInfo: ATAdInfo?) {
                if (rewardAd !== ad) return
                val showId = rewardShowId
                rewardShowId = null
                val outcome = if (showId != null) {
                    val elapsed = ((SystemClock.elapsedRealtime() - rewardPlayStartAt) / 1000).toInt()
                    RewardOutcome.Completed(showId, elapsed.coerceAtLeast(MIN_REWARD_SECONDS))
                } else {
                    RewardOutcome.ClosedEarly
                }
                log("激励视频关闭 rewarded=${showId != null}")
                finishReward(outcome)
            }

            override fun onRewardedVideoAdPlayClicked(adInfo: ATAdInfo?) {
                if (rewardAd !== ad) return
                log("激励视频被点击")
            }

            override fun onReward(adInfo: ATAdInfo?) {
                if (rewardAd !== ad) return
                // showId 每条广告唯一，直接作为服务端的去重标识
                rewardShowId = adInfo?.getShowId()
                    ?.takeIf { it.isNotBlank() }
                    ?.take(64)
                    ?: UUID.randomUUID().toString()
                log("激励视频发奖回调 showId=$rewardShowId")
            }
        })
        rewardAd = ad
        ad.load()
        log("已请求加载激励视频 placement=${BuildConfig.TAKU_REWARD_PLACEMENT_ID}")
    }

    /**
     * 展示激励视频并挂起等待结果。
     *
     * 只有 SDK 回调 onReward 才算完成；中途关闭返回 [RewardOutcome.ClosedEarly]，
     * 调用方不能据此发积分。
     */
    suspend fun playReward(activity: Activity): RewardOutcome {
        if (!rewardConfigured) return RewardOutcome.NotConfigured

        val ad = rewardAd
        if (ad == null || !ad.isAdReady()) {
            ensureRewardLoaded(activity)
            return RewardOutcome.NotReady
        }

        rewardShowId = null
        rewardPlayStartAt = SystemClock.elapsedRealtime()
        return suspendCancellableCoroutine { continuation ->
            rewardContinuation = continuation
            continuation.invokeOnCancellation {
                if (rewardContinuation === continuation) rewardContinuation = null
            }
            try {
                ad.show(activity)
            } catch (e: Throwable) {
                log("激励视频展示异常: ${e.message}")
                rewardContinuation = null
                rewardReady = false
                rewardAd = null
                ad.destroyAd()
                ensureRewardLoaded(activity)
                if (continuation.isActive) continuation.resume(RewardOutcome.ShowFailed)
            }
        }
    }

    /** 收尾：先回传结果，再销毁本次广告并补一条新的库存 */
    private fun finishReward(outcome: RewardOutcome) {
        val continuation = rewardContinuation
        rewardContinuation = null
        rewardShowId = null
        rewardReady = false
        rewardAd?.destroyAd()
        rewardAd = null
        appContext?.let { ensureRewardLoaded(it) }
        if (continuation?.isActive == true) continuation.resume(outcome)
    }

    /**
     * 预加载开屏。启动页展示期间并行调用，返回的 Deferred 在「加载成功 / 无填充 / 超时」时结算，
     * 调用方据此决定是展示广告还是直接进首页，避免出现「先到首页、再弹出广告」。
     */
    fun loadSplash(context: Context): Deferred<Boolean> {
        splashResult?.let { return it }
        val result = CompletableDeferred<Boolean>()
        splashResult = result

        if (BuildConfig.TAKU_SPLASH_PLACEMENT_ID.isBlank()) {
            log("未配置开屏广告位，跳过")
            result.complete(false)
            return result
        }
        if (splashAd != null) {
            result.complete(true)
            return result
        }

        val appContext = context.applicationContext
        val ad = ATSplashAd(
            appContext,
            BuildConfig.TAKU_SPLASH_PLACEMENT_ID,
            object : ATSplashAdListener {
                override fun onAdLoaded(isTimeout: Boolean) {
                    log("开屏加载成功 timeout=$isTimeout")
                    result.complete(true)
                }

                override fun onAdLoadTimeout() {
                    log("开屏加载超时")
                    result.complete(false)
                }

                override fun onNoAdError(error: AdError?) {
                    log("开屏加载失败 code=${error?.code} desc=${error?.desc}")
                    result.complete(false)
                }

                override fun onAdShow(adInfo: ATAdInfo?) {
                    log("开屏展示")
                }

                override fun onAdClick(adInfo: ATAdInfo?) {
                    log("开屏被点击")
                }

                override fun onAdDismiss(adInfo: ATAdInfo?, extraInfo: ATSplashAdExtraInfo?) {
                    log("开屏关闭 dismissType=${extraInfo?.dismissType}")
                    finishSplash()
                }
            },
        )
        // 部分模板会读取这两个值在广告底部画品牌区；客户端另有兜底品牌条
        ad.setLocalExtra(
            mapOf(
                ATAdConst.KEY.APP_LOGO_ID to R.drawable.brand_mark,
                ATAdConst.KEY.APP_NAME to appContext.getString(R.string.app_name),
            ),
        )
        splashAd = ad
        ad.loadAd()
        log("已请求加载开屏 placement=${BuildConfig.TAKU_SPLASH_PLACEMENT_ID}")
        return result
    }

    /** 在 [container] 内展示已就绪的开屏；关闭、广告失败或未就绪都会回调一次 [onFinished]。 */
    fun showSplash(activity: Activity, container: ViewGroup, onFinished: () -> Unit = {}) {
        val ad = splashAd
        if (ad == null || !ad.isAdReady()) {
            log("showSplash 跳过：ready=${ad?.isAdReady()}")
            onFinished()
            return
        }
        splashOnFinished = onFinished
        log("showSplash 调用 show")
        ad.show(activity, container)
    }

    /** 广告关闭后收尾：先回调再销毁，避免调用方在回调里重复 release */
    private fun finishSplash() {
        val callback = splashOnFinished
        splashOnFinished = null
        releaseSplash()
        callback?.invoke()
    }

    fun releaseSplash() {
        splashAd?.onDestory()
        splashAd = null
        splashResult = null
        splashOnFinished = null
    }

    internal fun log(message: String) {
        Timber.tag(TAG).d(message)
    }
}

/** 从 Compose 的 LocalContext 里找出承载广告的 Activity */
internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
