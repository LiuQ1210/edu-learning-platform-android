package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.RawTableData
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 签到模块 ApiV1 接口（对齐后端 ApiV1CheckinController）。
 *
 * 后端字段平铺：
 *  - sign     -> `{code,msg,points,continuousDays,totalDays,balance,signDate}`
 *  - status   -> `{signedToday,todayPoints,continuousDays,totalDays,balance,today}`
 *  - calendar -> `{year,month,days,today,signedToday}`
 *  - rules    -> `{tiers,loopDays,dailyLimit,dailyTip,resetNote,loopNote}`
 */
interface CheckinApi {

    /** 每日签到 */
    @POST("api/v1/checkin/sign")
    suspend fun sign(): Response<JsonObject>

    /** 签到状态 */
    @GET("api/v1/checkin/status")
    suspend fun getStatus(): Response<JsonObject>

    /** 签到日历 */
    @GET("api/v1/checkin/calendar")
    suspend fun getCalendar(
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null
    ): Response<JsonObject>

    /** 签到记录（分页） */
    @GET("api/v1/checkin/records")
    suspend fun getRecords(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<RawTableData>

    /** 签到积分规则 */
    @GET("api/v1/checkin/rules")
    suspend fun getRules(): Response<JsonObject>
}
