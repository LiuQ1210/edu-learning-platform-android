package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.data.local.TaskCenterStore
import com.github.learningplatform.data.remote.PointsApi
import com.github.learningplatform.data.remote.dto.AdRewardResult
import com.github.learningplatform.domain.model.TaskCenterRules
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 激励视频领分。
 *
 * 是否发分、每天 3 条上限、同一广告去重全部由服务端判定；
 * 客户端只把权威结果同步给 [TaskCenterStore] 用于任务中心展示。
 */
@Singleton
class AdRewardRepository @Inject constructor(
    private val pointsApi: PointsApi,
    private val taskCenterStore: TaskCenterStore
) {

    /**
     * @param adId    SDK 回调的广告 showId（每条广告唯一，≤64 字符）
     * @param seconds 本次观看秒数，服务端要求 15~3600
     */
    suspend fun claim(adId: String, seconds: Int): AdRewardResult {
        val normalizedId = adId.take(MAX_AD_ID_LENGTH)
        val body = pointsApi.reportAdReward(
            adId = normalizedId,
            seconds = seconds.coerceIn(AD_MIN_SECONDS, MAX_SECONDS)
        ).okBody().payload()

        val result = AdRewardResult(
            awardedPoints = body.int("awardedPoints"),
            todayAdCount = body.int("todayAdCount"),
            todayAdLimit = body.int("todayAdLimit")
                .takeIf { it > 0 }
                ?: TaskCenterRules.AD_DAILY_LIMIT
        )
        // 本地展示缓存沿用服务端口径：条数取 todayAdCount，积分由 ViewModel 展示为准
        taskCenterStore.applyAdResult(adId = normalizedId, awardedPoints = result.awardedPoints)
        return result
    }

    private companion object {
        const val MAX_AD_ID_LENGTH = 64
        const val AD_MIN_SECONDS = 15
        const val MAX_SECONDS = 3600
    }
}
