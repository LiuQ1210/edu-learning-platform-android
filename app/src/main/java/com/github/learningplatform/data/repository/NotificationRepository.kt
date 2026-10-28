package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.toAnyMap
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
    ): List<Map<String, Any>> =
        notificationApi.getNotificationList(isRead, pageNum, pageSize)
            .okTable()
            .rows
            .map { it.toAnyMap() }

    /** 未读数 */
    suspend fun getUnreadCount(): Int =
        notificationApi.getUnreadCount().okBody().payload().int("count")

    /** 标记单条已读 */
    suspend fun markRead(notificationId: Long): Int =
        notificationApi.markRead(notificationId).okBody().payload().int("count")

    /** 全部标记已读 */
    suspend fun markAllRead(): Int =
        notificationApi.markAllRead().okBody().payload().int("count")
}
