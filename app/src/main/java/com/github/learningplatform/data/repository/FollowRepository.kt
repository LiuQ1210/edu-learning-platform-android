package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.remote.FollowApi
import javax.inject.Inject
import javax.inject.Singleton

/** 关注仓库（对接后端 ApiV1 接口） */
@Singleton
class FollowRepository @Inject constructor(
    private val followApi: FollowApi
) {

    /** 关注某人 */
    suspend fun addFollow(targetUserId: Long): Map<String, Any> {
        val response = followApi.addFollow(targetUserId)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.message)
        }
        return body.data ?: emptyMap()
    }

    /** 取消关注 */
    suspend fun cancelFollow(targetUserId: Long): Map<String, Any> {
        val response = followApi.cancelFollow(targetUserId)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.message)
        }
        return body.data ?: emptyMap()
    }

    /** 批量查询已关注的用户ID */
    suspend fun getFollowStatus(userIds: List<Long>): List<Long> {
        val response = followApi.getFollowStatus(userIds.joinToString(","))
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.message)
        }
        val data = body.data ?: emptyMap()
        return data["ids"] ?: emptyList()
    }

    /** 查粉丝数+关注数 */
    suspend fun getFollowCount(userId: Long? = null): Map<String, Int> {
        val response = followApi.getFollowCount(userId)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.message)
        }
        return body.data ?: emptyMap()
    }
}
