package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.AdSlotDto
import com.github.learningplatform.data.remote.dto.AppConfigDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** 广告位与 APP 配置（接口文档 v3.0 13.1 / 14.1，均匿名可访问） */
interface AppApi {

    /** @param scene 选填：community/video/startup/global */
    @GET("ad-slots")
    suspend fun getAdSlots(
        @Query("scene") scene: String? = null
    ): Response<ApiResponse<List<AdSlotDto>>>

    @GET("app-config")
    suspend fun getAppConfig(): Response<ApiResponse<AppConfigDto>>
}