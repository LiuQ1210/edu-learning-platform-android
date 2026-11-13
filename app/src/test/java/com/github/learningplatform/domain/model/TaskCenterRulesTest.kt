package com.github.learningplatform.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 任务中心签到周期与每日上限的纯逻辑测试。
 *
 * 这些规则直接决定任务中心展示的 +N 分，必须和服务端口径一致：
 *  - 第 1~7 天 +1~+7；
 *  - 第 8 天开始新周期，回到 +1；
 *  - 断签后连续天数归零，下一次重新从第 1 天算；
 *  - 课程 + 帖子每日合计封顶 50 分；
 *  - 签到、广告不占用内容类 50 分额度。
 */
class TaskCenterRulesTest {

    @Test
    fun `signed cycle day repeats every seven days`() {
        assertEquals(1, TaskCenterRules.signedCycleDay(1))
        assertEquals(2, TaskCenterRules.signedCycleDay(2))
        assertEquals(7, TaskCenterRules.signedCycleDay(7))
        // 第二周期
        assertEquals(1, TaskCenterRules.signedCycleDay(8))
        assertEquals(5, TaskCenterRules.signedCycleDay(12))
        assertEquals(7, TaskCenterRules.signedCycleDay(14))
        assertEquals(1, TaskCenterRules.signedCycleDay(15))
    }

    @Test
    fun `next cycle day is based on current continuous days`() {
        assertEquals(1, TaskCenterRules.nextCycleDay(0))
        assertEquals(2, TaskCenterRules.nextCycleDay(1))
        assertEquals(5, TaskCenterRules.nextCycleDay(4))
        // 连续 7 天、今天未签：下一次是新周期第 1 天
        assertEquals(1, TaskCenterRules.nextCycleDay(7))
        assertEquals(6, TaskCenterRules.nextCycleDay(12))
    }

    @Test
    fun `content total is capped at fifty`() {
        val progress = DailyTaskProgress(rawContentPoints = 60)
        assertEquals(TaskCenterRules.CONTENT_DAILY_CAP, progress.contentPoints)
        assertEquals(0, progress.remainingContentPoints)
        assertEquals(true, progress.contentReachedCap)
    }

    @Test
    fun `checkin and ads are outside the content cap`() {
        val progress = DailyTaskProgress(
            checkinPoints = 7,
            rawContentPoints = 40,
            adPoints = 10,
            adCount = 1
        )
        assertEquals(40, progress.contentPoints)
        assertEquals(57, progress.totalPoints)
        assertEquals(10, progress.remainingContentPoints)
        assertEquals(false, progress.contentReachedCap)
    }

    @Test
    fun `ad task allows three per day`() {
        val progress = DailyTaskProgress(adCount = 3, adPoints = 30)
        assertEquals(0, progress.remainingAdCount)
        assertEquals(true, progress.adReachedLimit)
        assertEquals(30, progress.totalPoints)
    }

    @Test
    fun `content total never exceeds cap even with invalid stored values`() {
        val progress = DailyTaskProgress(rawContentPoints = 200)
        assertEquals(TaskCenterRules.CONTENT_DAILY_CAP, progress.contentPoints)
    }

    @Test
    fun `negative stored content points do not reduce today total`() {
        val progress = DailyTaskProgress(checkinPoints = 3, rawContentPoints = -10)
        assertEquals(0, progress.contentPoints)
        assertEquals(3, progress.totalPoints)
    }
}
