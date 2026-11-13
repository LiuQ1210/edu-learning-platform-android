package com.github.learningplatform.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 观看 / 阅读秒数累计器。
 *
 * 关键行为：
 *  - 只在真实播放/前台停留时累加，上报后清零；
 *  - 未满一个上报间隔不发送，但暂停/退出时强制发送；
 *  - 上报失败可以回填，不会丢秒数。
 */
class WatchAccumulatorTest {

    @Test
    fun `accumulates seconds until report interval`() {
        val acc = WatchAccumulator(reportIntervalSeconds = 3)
        assertFalse(acc.addSecond())
        assertFalse(acc.addSecond())
        assertTrue(acc.addSecond())
        assertEquals(WatchAccumulator.Report(seconds = 3, finished = false), acc.takeForReport(force = false))
        assertEquals(0, acc.pendingSeconds())
    }

    @Test
    fun `does not report before interval unless forced`() {
        val acc = WatchAccumulator(reportIntervalSeconds = 20)
        acc.addSecond()
        acc.addSecond()
        assertNull(acc.takeForReport(force = false))

        val report = acc.takeForReport(force = true)
        assertEquals(WatchAccumulator.Report(seconds = 2, finished = false), report)
    }

    @Test
    fun `force report returns null when nothing pending`() {
        val acc = WatchAccumulator(reportIntervalSeconds = 5)
        assertNull(acc.takeForReport(force = true))
    }

    @Test
    fun `finished flag travels with the report and resets afterwards`() {
        val acc = WatchAccumulator(reportIntervalSeconds = 5)
        acc.addSecond()
        acc.markFinished()
        val report = acc.takeForReport(force = true)
        assertEquals(WatchAccumulator.Report(seconds = 1, finished = true), report)

        acc.addSecond()
        assertEquals(WatchAccumulator.Report(seconds = 1, finished = false), acc.takeForReport(force = true))
    }

    @Test
    fun `failed report can be restored without losing seconds`() {
        val acc = WatchAccumulator(reportIntervalSeconds = 3)
        repeat(3) { acc.addSecond() }
        val report = acc.takeForReport(force = false)!!
        acc.restore(report)
        assertEquals(3, acc.pendingSeconds())

        acc.addSecond()
        assertEquals(WatchAccumulator.Report(seconds = 4, finished = false), acc.takeForReport(force = true))
    }

    @Test
    fun `pending seconds are capped at server limit`() {
        val acc = WatchAccumulator(reportIntervalSeconds = 100_000)
        repeat(WatchAccumulator.MAX_SECONDS + 10) { acc.addSecond() }
        assertEquals(WatchAccumulator.MAX_SECONDS, acc.pendingSeconds())
    }
}
