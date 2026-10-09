package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.contentPreview
import com.github.learningplatform.data.remote.CheckinApi
import com.github.learningplatform.data.remote.dto.CheckinRecordDto
import com.github.learningplatform.data.remote.dto.CheckinResultDto
import com.github.learningplatform.data.remote.dto.CheckinSubmitRequest
import com.github.learningplatform.data.remote.dto.CheckinTaskDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.SignCalendarDto
import com.github.learningplatform.data.remote.UserApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 签到仓库（接口文档 v3.0 第九章 + 3.12 签到日历）。
 * 距离/手势/时间窗校验均由后端完成。
 */
@Singleton
class CheckinRepository @Inject constructor(
    private val checkinApi: CheckinApi,
    private val userApi: UserApi
) {

    /** 今日待签到任务 */
    suspend fun getTodayTasks(): List<CheckinTaskDto> {
        contentPreview { return DemoData.checkinTasks }
        return safeApiCall { checkinApi.getTodayTasks() }
    }

    suspend fun submit(
        taskId: Long,
        longitude: Double? = null,
        latitude: Double? = null,
        gesturePattern: String? = null,
        photoUrl: String? = null
    ): CheckinResultDto {
        contentPreview { return DemoData.checkinResult("签到任务") }
        return safeApiCall {
            checkinApi.submit(
                CheckinSubmitRequest(
                    taskId = taskId,
                    longitude = longitude,
                    latitude = latitude,
                    gesturePattern = gesturePattern,
                    photoUrl = photoUrl
                )
            )
        }
    }

    suspend fun getRecords(
        pageNum: Int = 1,
        pageSize: Int = 20,
        taskId: Long? = null
    ): PageData<CheckinRecordDto> {
        contentPreview { return DemoData.checkinRecords }
        return safeApiCall {
            checkinApi.getRecords(pageNum = pageNum, pageSize = pageSize, taskId = taskId)
        }
    }

    /** 3.12 签到日历（月度统计） */
    suspend fun getSignCalendar(year: Int, month: Int): SignCalendarDto {
        contentPreview { return DemoData.signCalendar }
        return safeApiCall { userApi.getSignCalendar(year, month) }
    }
}
