package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.longList
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.data.remote.FollowApi
import javax.inject.Inject
import javax.inject.Singleton

/** 关注仓库（对接后端 ApiV1 接口，字段平铺在 AjaxResult 顶层） */
@Singleton
class FollowRepository @Inject constructor(
    private val followApi: FollowApi
) {

    /** 关注某人 */
    suspend fun addFollow(targetUserId: Long): Map<String, Any> {
        followApi.addFollow(targetUserId).okBody()
        return emptyMap()
    }

    /** 取消关注 */
    suspend fun cancelFollow(targetUserId: Long): Map<String, Any> {
        followApi.cancelFollow(targetUserId).okBody()
        return emptyMap()
    }

    /** 批量查询已关注的用户ID */
    suspend fun getFollowStatus(userIds: List<Long>): List<Long> {
        val body = followApi.getFollowStatus(userIds.joinToString(",")).okBody()
        // 后端：ajax.put("ids", ...)
        return body.longList("ids").ifEmpty { body.payload().longList("ids") }
    }

    /** 查粉丝数 + 关注数 */
    suspend fun getFollowCount(userId: Long? = null): Map<String, Int> {
        val body = followApi.getFollowCount(userId).okBody().payload()
        return mapOf(
            "fansCount" to body.int("fansCount"),
            "followCount" to body.int("followCount")
        )
    }
}
