package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.TableDataInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** 通知模块 ApiV1 接口 */
interface NotificationApi {

    /** 我的通知列表（分页） */
    @GET("api/v1/notification/list")
    suspend fun getNotificationList(
        @Query("isRead") isRead: String? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<TableDataInfo<Map<String, Any>>>

    /** 我的未读数 */
    @GET("api/v1/notification/unreadCount")
    suspend fun getUnreadCount(): Response<ApiResponse<Map<String, Int>>>

    /** 标记单条已读 */
    @POST("api/v1/notification/read")
    suspend fun markRead(
        @Query("notificationId") notificationId: Long
    ): Response<ApiResponse<Map<String, Int>>>

    /** 全部标记已读 */
    @POST("api/v1/notification/readAll")
    suspend fun markAllRead(): Response<ApiResponse<Map<String, Int>>>
}
