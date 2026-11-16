package com.github.learningplatform.data.ad

import android.app.Activity
import android.content.Context
import android.os.Build
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
import com.anythink.splashad.api.ATSplashAd
import com.anythink.splashad.api.ATSplashAdExtraInfo
import com.anythink.splashad.api.ATSplashAdListener
import com.github.learningplatform.BuildConfig
import com.github.learningplatform.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import timber.log.Timber
import java.util.UUID

/**
 * Taku 聚合广告（当前只有酷盈 ADX）的接入点。
 *
 * 所有回调节点统一打 `TakuTest` 标签的日志，调试阶段直接看 Logcat 验证链路。
 */
object TakuAds {
    private const val TAG = "TakuTest"

    /**
     * 供 UI 感知初始化状态：横幅要等隐私同意、SDK 初始化完成后才能请求。
     * 用 Compose 状态承载，同意后社区页会自动挂上横幅。
     */
    var initialized by mutableStateOf(false)
        private set

    private var splashAd: ATSplashAd? = null

    /** 本次开屏加载的结果：true=可展示，false=无填充/超时。重复调用复用同一个实例 */
    private var splashResult: CompletableDeferred<Boolean>? = null

    private var splashOnFinished: (() -> Unit)? = null

    fun init(context: Context) {
        if (initialized) return
        val appContext = context.applicationContext
        ATSDK.init(appContext, BuildConfig.TAKU_APP_ID, BuildConfig.TAKU_APP_KEY)
        ATSDK.start()
        initialized = true
        log("SDK 初始化完成，版本 ${ATSDK.getSDKVersionName()}")

        if (BuildConfig.DEBUG) {
            ATSDK.setNetworkLogDebug(true)
            provideFallbackOaid(appContext)
        }
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
