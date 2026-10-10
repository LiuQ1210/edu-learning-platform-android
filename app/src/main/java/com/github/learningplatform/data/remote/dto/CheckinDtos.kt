package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

// ---------------------- 9. 每日签到（平台积分体系） ----------------------

/** 9.1 签到状态 */
@Serializable
data class DailySignStatusDto(
    /** 今日是否已签到 */
    val signedToday: Boolean = false,
    /** 连续签到天数 */
    val continuousDays: Int = 0,
    /** 积分余额 */
    val points: Int = 0,
    /** 累计签到次数 */
    val totalSigns: Int = 0,
    /** 免广告到期时间戳（毫秒），0 表示当前无免广告权益 */
    val adFreeUntil: Long = 0L,
    /** 今日签到可得积分 */
    val todayPoints: Int = 5
)

/** 9.2 签到结果 */
@Serializable
data class DailySignResultDto(
    val success: Boolean = false,
    val message: String = "",
    /** 本次获得积分 */
    val earnedPoints: Int = 0,
    /** 当前连续签到天数 */
    val continuousDays: Int = 0,
    /** 当前积分余额 */
    val points: Int = 0
)

/** 9.3 免广告兑换选项 */
@Serializable
data class AdFreeRewardDto(
    val rewardId: String = "",
    val label: String = "",
    /** 所需积分 */
    val costPoints: Int = 0,
    /** 免广告时长（分钟） */
    val durationMinutes: Int = 0
)

/** 9.4 兑换结果 */
@Serializable
data class ExchangeResultDto(
    val success: Boolean = false,
    val message: String = "",
    val pointsLeft: Int = 0,
    val adFreeUntil: Long = 0L
) {
    companion object {
        val OPTIONS = listOf(
            AdFreeRewardDto("30min", "免广告 30 分钟", 10, 30),
            AdFreeRewardDto("2h", "免广告 2 小时", 30, 120),
            AdFreeRewardDto("24h", "免广告 24 小时", 100, 1440)
        )
    }
}
