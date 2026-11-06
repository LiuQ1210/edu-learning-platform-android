package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.double
import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.RatingApi
import com.github.learningplatform.data.remote.dto.RatingDto
import com.github.learningplatform.data.remote.dto.RatingResultDto
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    /** 新增评分（content 可选） */
    suspend fun addRating(courseId: Long, score: Int, content: String? = null): RatingResultDto {
        val data = ratingApi.addRating(courseId, score, content).okBody().payload()
        return RatingResultDto(
            averageScore = data.double("avgScore"),
            ratingCount = data.int("ratingCount")
        )
    }

    /** 修改评分（content 可选，空串=清空文字） */
    suspend fun updateRating(courseId: Long, score: Int, content: String? = null): RatingResultDto {
        val data = ratingApi.updateRating(courseId, score, content).okBody().payload()
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

    /** 评分列表（后端返回 TableDataInfo：{total, rows:[...]}） */
    suspend fun getRatingList(courseId: Long): List<RatingDto> {
        val root = ratingApi.getRatingList(courseId).okBody()
        val rows = root["rows"]?.jsonArray ?: return emptyList()
        return rows.map { el ->
            val o = el.jsonObject
            RatingDto(
                userId = o.long("userId"),
                userName = o.str("userName"),
                userAvatar = o.str("avatar").ifBlank { o.str("userAvatar") },
                score = o.double("score"),
                comment = o.str("content"),
                createTime = o.str("createTime")
            )
        }
    }
}
