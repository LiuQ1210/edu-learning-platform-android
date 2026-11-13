package com.github.learningplatform.domain.model

/**
 * 观看 / 阅读秒数累计器。
 *
 * 只累计「真实播放」或「页面前台」的秒数，不按播放位置差值计算，
 * 避免拖动进度条被当成观看时长。每累计 [DEFAULT_REPORT_INTERVAL_SECONDS] 秒
 * 取出一批增量上报；上报失败可原样回填，下次继续报。
 *
 * 方法均为同步操作，供主线程累加、后台协程取数，调用方不需要额外加锁。
 */
class WatchAccumulator(
    private val reportIntervalSeconds: Int = DEFAULT_REPORT_INTERVAL_SECONDS
) {

    private var pendingSeconds = 0
    private var finished = false

    /** 累计一秒；返回 true 表示已到上报间隔，调用方应触发一次上报。 */
    @Synchronized
    fun addSecond(): Boolean {
        pendingSeconds = (pendingSeconds + 1).coerceAtMost(MAX_SECONDS)
        return pendingSeconds >= reportIntervalSeconds
    }

    /** 标记已看完；下一次上报会带上 finished=true。 */
    @Synchronized
    fun markFinished() {
        finished = true
    }

    /**
     * 取出一批待上报秒数。
     *
     * @param force true 时不足一个间隔也取出（暂停、退出、播放结束）
     * @return 没有可上报秒数时返回 null
     */
    @Synchronized
    fun takeForReport(force: Boolean): Report? {
        if (pendingSeconds <= 0) return null
        if (!force && pendingSeconds < reportIntervalSeconds) return null
        val report = Report(seconds = pendingSeconds, finished = finished)
        pendingSeconds = 0
        finished = false
        return report
    }

    /** 上报失败时把秒数与完成标记放回，等待下次重试。 */
    @Synchronized
    fun restore(report: Report) {
        pendingSeconds = (pendingSeconds + report.seconds).coerceAtMost(MAX_SECONDS)
        if (report.finished) finished = true
    }

    @Synchronized
    fun pendingSeconds(): Int = pendingSeconds

    data class Report(val seconds: Int, val finished: Boolean)

    companion object {
        /** 服务端单次上报上限（1 小时） */
        const val MAX_SECONDS = 3600

        /** 上报间隔：与播放页原有心跳保持一致 */
        const val DEFAULT_REPORT_INTERVAL_SECONDS = 20
    }
}
