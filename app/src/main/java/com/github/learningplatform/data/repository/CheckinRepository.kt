package com.github.learningplatform.data.repository

import com.github.learningplatform.data.remote.dto.AdFreeRewardDto
import com.github.learningplatform.data.remote.dto.DailySignResultDto
import com.github.learningplatform.data.remote.dto.DailySignStatusDto
import com.github.learningplatform.data.remote.dto.ExchangeResultDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * 每日签到仓库（平台积分体系）。
 *
 * preview/离线模式下用内存状态模拟，不依赖后端：
 * - 每天只能签到一次（按本地日期判断）
 * - 连续签到递增积分：5/8/10/12/15/18/20，第7天后循环
 * - 积分可兑换免广告时长
 */
@Singleton
class CheckinRepository @Inject constructor() {

    // ---- preview 内存状态 ----
    private var lastSignDate: String = ""
    private var continuousDays: Int = 0
    private var points: Int = 0
    private var totalSigns: Int = 0
    private var adFreeUntil: Long = 0L

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private fun today(): String = dateFmt.format(Date())

    /** 连续签到第 N 天（1~7）对应的积分 */
    private fun pointsForDay(day: Int): Int {
        val cycle = ((day - 1) % 7) + 1
        return when (cycle) {
            1 -> 5
            2 -> 8
            3 -> 10
            4 -> 12
            5 -> 15
            6 -> 18
            else -> 20
        }
    }

    suspend fun getStatus(): DailySignStatusDto {
        val today = today()
        val signedToday = lastSignDate == today
        return DailySignStatusDto(
            signedToday = signedToday,
            continuousDays = continuousDays,
            points = points,
            totalSigns = totalSigns,
            adFreeUntil = adFreeUntil,
            todayPoints = pointsForDay(continuousDays + 1)
        )
    }

    suspend fun sign(): DailySignResultDto {
        val today = today()
        if (lastSignDate == today) {
            return DailySignResultDto(success = false, message = "今日已签到", points = points)
        }
        // 判断昨天是否签到，决定连续天数是否重置
        val yesterday = dateFmt.format(Date(System.currentTimeMillis() - 24 * 3600 * 1000))
        continuousDays = if (lastSignDate == yesterday) continuousDays + 1 else 1
        val earned = pointsForDay(continuousDays)
        points += earned
        totalSigns += 1
        lastSignDate = today
        return DailySignResultDto(
            success = true,
            message = "签到成功，+$earned 积分",
            earnedPoints = earned,
            continuousDays = continuousDays,
            points = points
        )
    }

    suspend fun exchange(reward: AdFreeRewardDto): ExchangeResultDto {
        if (points < reward.costPoints) {
            return ExchangeResultDto(success = false, message = "积分不足", pointsLeft = points, adFreeUntil = adFreeUntil)
        }
        points -= reward.costPoints
        val now = System.currentTimeMillis()
        val base = max(adFreeUntil, now)
        adFreeUntil = base + reward.durationMinutes * 60 * 1000L
        return ExchangeResultDto(
            success = true,
            message = "兑换成功，免广告 ${reward.durationMinutes} 分钟",
            pointsLeft = points,
            adFreeUntil = adFreeUntil
        )
    }

    fun rewards(): List<AdFreeRewardDto> = ExchangeResultDto.OPTIONS
}
