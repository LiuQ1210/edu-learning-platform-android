package com.github.learningplatform.domain.model

/**
 * 任务中心积分规则（客户端展示与本地进度计算的唯一事实来源）。
 *
 * 规则口径：
 *  - 签到：7 天一个周期，周期内第 1~7 天分别 +1~+7 分；一个周期结束后
 *    从新周期第 1 天重新计算（+1 分），连续签到天数仍累计；周期内断签，
 *    本周期作废，从断签后的第一次签到起重新从第 1 天算；每天最多 1 条记录。
 *  - 观看课程视频：仅课程小节计入积分，短视频不计分；单个课程小节观看满 30 秒 +5 分，
 *    同一小节每天只计 1 次。
 *  - 浏览帖子：社区文章 / 帖子浏览满 30 秒 +5 分，同一帖子每天只计 1 次。
 *  - 课程小节 + 帖子两类内容每日合计不超过 50 分。
 *  - 观看广告：完整看完一条广告 +10 分，同一广告内容每天只计 1 次，
 *    每天最多 3 条；广告积分不占用内容类 50 分额度。
 *  - 签到积分单独计算，不占用内容类 50 分额度。
 */
object TaskCenterRules {

    /** 签到周期天数 */
    const val CHECKIN_CYCLE_DAYS = 7

    /** 课程视频 / 文章计入积分所需的最短观看时长（秒）；短视频不计分 */
    const val MIN_WATCH_SECONDS = 30

    /** 单个课程小节可获得的积分；短视频不再获得积分 */
    const val VIDEO_REWARD = 5

    /** 单篇帖子可获得的积分 */
    const val ARTICLE_REWARD = 5

    /** 单条广告完整观看可获得的积分 */
    const val AD_REWARD = 10

    /** 课程小节 + 帖子两类内容每日合计积分上限 */
    const val CONTENT_DAILY_CAP = 50

    /** 广告每天最多计分条数 */
    const val AD_DAILY_LIMIT = 3

    /**
     * 已签到 N 天后，这次签到属于本周期第几天（1..7）。
     *
     * 例：连续第 7 天 -> 7；连续第 8 天（第二周期第 1 天）-> 1。
     */
    fun signedCycleDay(continuousDays: Int): Int {
        val days = continuousDays.coerceAtLeast(1)
        return (days - 1) % CHECKIN_CYCLE_DAYS + 1
    }

    /**
     * 已连续签到 N 天、今天还没签到时，下一次签到属于本周期第几天（1..7）。
     *
     * 例：连续 7 天后未签 -> 下一次是第二周期第 1 天；连续 12 天 -> 第 6 天。
     */
    fun nextCycleDay(continuousDays: Int): Int =
        continuousDays.coerceAtLeast(0) % CHECKIN_CYCLE_DAYS + 1

    /** 已签到状态下本次签到获得的积分（1..7） */
    fun checkinPointsForSignedDay(continuousDays: Int): Int =
        signedCycleDay(continuousDays)

    /** 未签到状态下下一次签到预计获得的积分（1..7） */
    fun nextCheckinPoints(continuousDays: Int): Int =
        nextCycleDay(continuousDays)
}

/** 内容类任务：课程视频 / 帖子，两者共享每日 50 分额度。 */
enum class ContentTaskType { VIDEO, ARTICLE }

/**
 * 今日任务进度（客户端展示缓存）。
 *
 * 积分是否发放由服务端判定，[rawContentPoints] 存的是服务端返回的
 * `todayContentPoints`；本地只额外记录「今日已计分的内容数量」用于展示。
 * 积分余额仍以服务端为准，这里不做任何余额加减。
 */
data class DailyTaskProgress(
    val date: String = "",
    /** 今日签到已得积分 */
    val checkinPoints: Int = 0,
    /** 今日签到是否已入账（防止刷新重复累加） */
    val checkinRecorded: Boolean = false,
    /** 服务端返回的今日课程 + 帖子积分（原值） */
    val rawContentPoints: Int = 0,
    /** 今日已计分的课程小节数 */
    val videoCount: Int = 0,
    /** 今日已计分的帖子数 */
    val articleCount: Int = 0,
    val adPoints: Int = 0,
    /** 今日已计分的广告数 */
    val adCount: Int = 0
) {
    /** 课程 + 帖子今日合计积分，封顶 [TaskCenterRules.CONTENT_DAILY_CAP] */
    val contentPoints: Int
        get() = rawContentPoints.coerceIn(0, TaskCenterRules.CONTENT_DAILY_CAP)

    /** 内容类今日剩余可得分 */
    val remainingContentPoints: Int
        get() = TaskCenterRules.CONTENT_DAILY_CAP - contentPoints

    val contentReachedCap: Boolean
        get() = contentPoints >= TaskCenterRules.CONTENT_DAILY_CAP

    /** 今日任务积分合计：签到 + 内容类 + 广告，后两者不互相占用额度 */
    val totalPoints: Int
        get() = checkinPoints.coerceAtLeast(0) + contentPoints + adPoints.coerceAtLeast(0)

    /** 今日还可计分的广告条数 */
    val remainingAdCount: Int
        get() = (TaskCenterRules.AD_DAILY_LIMIT - adCount).coerceAtLeast(0)

    val adReachedLimit: Boolean
        get() = adCount >= TaskCenterRules.AD_DAILY_LIMIT
}

/**
 * 一次任务计分结果。
 *
 * @param awarded 实际入账积分（达到每日上限时可能为 0，或少于任务名义分值）
 * @param alreadyDone 该内容今天 / 之前已经计过分
 * @param capped 今日积分已达上限
 */
data class TaskRewardResult(
    val awarded: Int = 0,
    val alreadyDone: Boolean = false,
    val capped: Boolean = false
)
