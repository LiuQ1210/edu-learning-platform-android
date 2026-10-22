package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.remote.NotificationApi
import javax.inject.Inject
import javax.inject.Singleton

/** 通知仓库（对接后端 ApiV1 接口） */
@Singleton
class NotificationRepository @Inject constructor(
    private val notificationApi: NotificationApi
) {

    /** 我的通知列表 */
    suspend fun getNotificationList(
        isRead: String? = null,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): List<Map<String, Any>> {
        val response = notificationApi.getNotificationList(isRead, pageNum, pageSize)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.msg)
        }
        return body.rows
    }

    /** 未读数 */
    suspend fun getUnreadCount(): Int {
        val response = notificationApi.getUnreadCount()
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.message)
        }
        val data = body.data ?: emptyMap()
        return data["count"] ?: 0
    }

    /** 标记单条已读 */
    suspend fun markRead(notificationId: Long): Int {
        val response = notificationApi.markRead(notificationId)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.message)
        }
        val data = body.data ?: emptyMap()
        return data["count"] ?: 0
    }

    /** 全部标记已读 */
    suspend fun markAllRead(): Int {
        val response = notificationApi.markAllRead()
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.message)
        }
        val data = body.data ?: emptyMap()
        return data["count"] ?: 0
    }
}
