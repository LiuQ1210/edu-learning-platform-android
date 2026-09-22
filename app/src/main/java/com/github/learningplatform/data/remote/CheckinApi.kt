package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.CheckinRecordDto
import com.github.learningplatform.data.remote.dto.CheckinResultDto
import com.github.learningplatform.data.remote.dto.CheckinSubmitRequest
import com.github.learningplatform.data.remote.dto.CheckinTaskDto
import com.github.learningplatform.data.remote.dto.PageData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** 签到模块 APP 端（接口文档 v3.0 9.1-9.3） */
interface CheckinApi {

    /** 9.1 今日待签到任务（返回数组，非分页） */
    @GET("checkins/tasks/today")
    suspend fun getTodayTasks(): Response<ApiResponse<List<CheckinTaskDto>>>

    /** 9.2 提交签到（按 checkinType 传对应字段） */
    @POST("checkins")
    suspend fun submit(
        @Body body: CheckinSubmitRequest
    ): Response<ApiResponse<CheckinResultDto>>

    /** 9.3 签到记录 */
    @GET("checkins/records")
    suspend fun getRecords(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("taskId") taskId: Long? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ApiResponse<PageData<CheckinRecordDto>>>
}