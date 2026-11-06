package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.double
import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.data.remote.RatingApi
import com.github.learningplatform.data.remote.dto.RatingResultDto
import javax.inject.Inject
import javax.inject.Singleton

/** 当前登录用户在某门课上的评分与课程汇总（后端 `GET /api/v1/rating/info`）。 */
data class CourseRatingInfo(
    val myScore: Double = 0.0,
    val averageScore: Double = 0.0,
    val ratingCount: Int = 0
)

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

    /**
     * 查询评分信息：myScore / avgScore / ratingCount。
     *
     * 后端可能把字段平铺在顶层，也可能包在 data 里，`payload()` 已统一处理。
     * 未评分时 `myScore` 可能是 0、null 或空串，这里统一归一成 0.0。
     */
    suspend fun getRatingInfo(courseId: Long): CourseRatingInfo {
        val data = ratingApi.getRatingInfo(courseId).okBody().payload()
        return CourseRatingInfo(
            myScore = data.double("myScore"),
            averageScore = data.double("avgScore"),
            ratingCount = data.int("ratingCount")
        )
    }
}
