package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.remote.RatingApi
import com.github.learningplatform.data.remote.dto.RatingResultDto
import javax.inject.Inject
import javax.inject.Singleton

/** 评分仓库（对接后端 ApiV1 接口） */
@Singleton
class RatingRepository @Inject constructor(
    private val ratingApi: RatingApi
) {

    /** 新增评分 */
    suspend fun addRating(courseId: Long, score: Int): RatingResultDto {
        val response = ratingApi.addRating(courseId, score)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.message)
        }
        val data = body.data ?: emptyMap()
        val avgScore = (data["avgScore"] as? Number)?.toDouble() ?: 0.0
        val ratingCount = (data["ratingCount"] as? Number)?.toInt() ?: 0
        return RatingResultDto(averageScore = avgScore, ratingCount = ratingCount)
    }

    /** 修改评分 */
    suspend fun updateRating(courseId: Long, score: Int): RatingResultDto {
        val response = ratingApi.updateRating(courseId, score)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.message)
        }
        val data = body.data ?: emptyMap()
        val avgScore = (data["avgScore"] as? Number)?.toDouble() ?: 0.0
        val ratingCount = (data["ratingCount"] as? Number)?.toInt() ?: 0
        return RatingResultDto(averageScore = avgScore, ratingCount = ratingCount)
    }

    /** 查询评分信息 */
    suspend fun getRatingInfo(courseId: Long): Map<String, Any> {
        val response = ratingApi.getRatingInfo(courseId)
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
