package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.double
import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.toAnyMap
import com.github.learningplatform.data.remote.RatingApi
import com.github.learningplatform.data.remote.dto.RatingResultDto
import javax.inject.Inject
import javax.inject.Singleton

/** 评分仓库（对接后端 ApiV1 接口，字段平铺） */
@Singleton
class RatingRepository @Inject constructor(
    private val ratingApi: RatingApi
) {

    /** 新增评分 */
    suspend fun addRating(courseId: Long, score: Int): RatingResultDto {
        val data = ratingApi.addRating(courseId, score).okBody().payload()
        return RatingResultDto(
            averageScore = data.double("avgScore"),
            ratingCount = data.int("ratingCount")
        )
    }

    /** 修改评分 */
    suspend fun updateRating(courseId: Long, score: Int): RatingResultDto {
        val data = ratingApi.updateRating(courseId, score).okBody().payload()
        return RatingResultDto(
            averageScore = data.double("avgScore"),
            ratingCount = data.int("ratingCount")
        )
    }

    /** 查询评分信息：myScore / avgScore / ratingCount */
    suspend fun getRatingInfo(courseId: Long): Map<String, Any> =
        ratingApi.getRatingInfo(courseId).okBody().payload().toAnyMap()
}
