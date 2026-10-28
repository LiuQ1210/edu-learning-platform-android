package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.RawTableData
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 通知模块 ApiV1 接口（对齐后端 ApiV1NotificationController）。
 *
 * 未读数/标记已读返回平铺的 `count` 字段。
 */
interface NotificationApi {

    /** 我的通知列表（分页） */
    @GET("api/v1/notification/list")
    suspend fun getNotificationList(
        @Query("isRead") isRead: String? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<RawTableData>

    /** 我的未读数，返回平铺字段 count */
    @GET("api/v1/notification/unreadCount")
    suspend fun getUnreadCount(): Response<JsonObject>

    /** 标记单条已读，返回平铺字段 count */
    @POST("api/v1/notification/read")
    suspend fun markRead(
        @Query("notificationId") notificationId: Long
    ): Response<JsonObject>

    /** 全部标记已读，返回平铺字段 count */
    @POST("api/v1/notification/readAll")
    suspend fun markAllRead(): Response<JsonObject>
}
