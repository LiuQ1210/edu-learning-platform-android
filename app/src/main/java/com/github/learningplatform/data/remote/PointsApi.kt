package com.github.learningplatform.data.remote

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 积分账户 / 免广告兑换接口（对齐后端 ApiV1PointsController）。
 *
 *  - `GET  /api/v1/points/account`         余额 + 免广告权益
 *  - `GET  /api/v1/points/exchange/tiers`  兑换档位 `{tiers:[{tier,cost,minutes,label}]}`
 *  - `POST /api/v1/points/exchange?tier=N` 兑换（服务端扣积分 + 权益叠加）
 *
 * 免广告状态一律以服务端 `adFree` / `adFreeRemainMinutes` 为准，
 * 客户端不要拿本地时间减 `adFreeExpireTime`。
 */
interface PointsApi {

    /** 我的积分账户 */
    @GET("api/v1/points/account")
    suspend fun getAccount(): Response<JsonObject>

    /** 免广告兑换档位 */
    @GET("api/v1/points/exchange/tiers")
    suspend fun getExchangeTiers(): Response<JsonObject>

    /** 兑换免广告 */
    @POST("api/v1/points/exchange")
    suspend fun exchange(
        @Query("tier") tier: Int
    ): Response<JsonObject>
}
