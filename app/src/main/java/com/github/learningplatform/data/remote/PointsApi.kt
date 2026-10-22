package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** 积分模块 ApiV1 接口 */
interface PointsApi {

    /** 我的积分账户（余额 + 免广告权益状态） */
    @GET("api/v1/points/account")
    suspend fun getAccount(): Response<ApiResponse<Map<String, Any>>>

    /** 免广告兑换档位 */
    @GET("api/v1/points/exchange/tiers")
    suspend fun getExchangeTiers(): Response<ApiResponse<Map<String, Any>>>

    /** 兑换免广告 */
    @POST("api/v1/points/exchange")
    suspend fun exchange(
        @Query("tier") tier: Int
    ): Response<ApiResponse<Map<String, Any>>>
}
