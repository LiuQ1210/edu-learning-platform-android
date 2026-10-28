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
        /** 仅作为后端 `/points/exchange/tiers` 拉取失败时的兜底展示 */
        val OPTIONS = listOf(
            AdFreeRewardDto("1", "免广告 30 分钟", 10, 30),
            AdFreeRewardDto("2", "免广告 2 小时", 30, 120),
            AdFreeRewardDto("3", "免广告 24 小时", 100, 1440)
        )
    }
}

// ---------------------------------------------------------------------------
// 签到 / 积分（对齐后端 ApiV1CheckinController + ApiV1PointsController）
// ---------------------------------------------------------------------------

/** 签到记录项（`GET /api/v1/checkin/records`） */
@Serializable
data class CheckinRecordDto(
    val checkinId: Long = 0,
    val signDate: String = "",
    val points: Int = 0,
    val continuousDays: Int = 0,
    val createTime: String = ""
)

/** 某月签到日历（`GET /api/v1/checkin/calendar`） */
@Serializable
data class CheckinCalendarDto(
    val year: Int = 0,
    val month: Int = 0,
    /** 本月已签到的「日」，例如 [20, 27] */
    val days: List<Int> = emptyList(),
    /** 服务端今天（yyyy-MM-dd），不要用手机本地时间 */
    val today: String = "",
    val signedToday: Boolean = false
)

/** 签到档位：连续第 N 天可得多少分 */
@Serializable
data class CheckinTierDto(val day: Int = 0, val points: Int = 0)

/** 签到规则（`GET /api/v1/checkin/rules`） */
@Serializable
data class CheckinRulesDto(
    val tiers: List<CheckinTierDto> = emptyList(),
    val loopDays: Int = 7,
    val dailyLimit: Int = 1,
    val dailyTip: String = "",
    val resetNote: String = "",
    val loopNote: String = ""
)

/** 积分账户（`GET /api/v1/points/account`） */
@Serializable
data class PointsAccountDto(
    val balance: Int = 0,
    /** 服务端算好的免广告状态，客户端不要用本地时间判断 */
    val adFree: Boolean = false,
    val adFreeExpireTime: String? = null,
    val adFreeRemainMinutes: Int = 0
)
