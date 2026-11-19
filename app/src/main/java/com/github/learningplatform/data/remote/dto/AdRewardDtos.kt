package com.github.learningplatform.data.remote.dto

/**
 * 激励视频结算结果（`POST /api/v1/points/ad/reward`）。
 *
 * 服务端按广告 id + 日期去重，并在每日 3 条上限处停止发分；
 * 客户端只负责展示，不再本地累加积分。
 */
data class AdRewardResult(
    /** 本次实际入账积分：0 或 10 */
    val awardedPoints: Int = 0,
    /** 当天已计分的广告条数（服务端权威值） */
    val todayAdCount: Int = 0,
    /** 每天最多计分的广告条数，固定 3 */
    val todayAdLimit: Int = 0
)
