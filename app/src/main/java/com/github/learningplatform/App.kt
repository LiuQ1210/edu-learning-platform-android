package com.github.learningplatform

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            // 开发期全量日志
            Timber.plant(Timber.DebugTree())
        } else {
            /*
             * 生产环境只记 WARN 及以上。
             *
             * 为什么不能像原来那样「release 什么都不记」：
             * 项目里所有排查性日志都走 Timber（缓存回源失败、播放器状态、
             * 播放失败原因等）。生产构建完全不记，等于线上出问题只剩崩溃栈，
             * 而这类问题（缓存读不出、视频加载失败）恰恰**不会崩溃**，只会表现成
             * 「功能不好使」——没有日志就无从定位。
             *
             * 只记 WARN+ 是为了控制成本：INFO/DEBUG 级别的日志量在线上会很大，
             * 真要全量采集应该接崩溃/日志平台，而不是打 Logcat。
             */
            Timber.plant(ReleaseTree())
        }
    }
}

/**
 * 生产日志树：只输出 WARN 及以上，并按级别映射到对应的 [Log] 方法。
 *
 * 不继承 `Timber.DebugTree` —— 那个会输出全部级别，等于把开发期日志带上线。
 */
private class ReleaseTree : Timber.Tree() {
    override fun isLoggable(tag: String?, priority: Int): Boolean =
        priority >= Log.WARN

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Logcat 单条上限约 4KB，超长会被截断；这里分段输出，避免关键堆栈被切掉
        val maxLen = 3000
        if (message.length <= maxLen) {
            Log.println(priority, tag ?: "App", message)
        } else {
            message.chunked(maxLen).forEach { Log.println(priority, tag ?: "App", it) }
        }
        t?.let { Log.println(priority, tag ?: "App", Log.getStackTraceString(it)) }
    }
}
