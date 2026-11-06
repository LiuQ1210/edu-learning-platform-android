package com.github.learningplatform.data.remote

import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.TableDataInfo
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 课程模块 APP 端 ApiV1 接口。
 */
interface CourseApi {

    /**
     * 课程列表（分页）
     * @param courseName 课程名称（模糊搜索）
     * @param categoryId 分类ID
     * @param sortBy 排序：likeCount / createTime / avgScore
     * @param pageNum 页码
     * @param pageSize 每页数量
     */
    @GET("api/v1/course/list")
    suspend fun getCourseList(
        @Query("courseName") courseName: String? = null,
        @Query("categoryId") categoryId: Long? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<TableDataInfo<CourseDto>>

    /**
     * 课程详情（对齐后端 `GET /api/v1/course/{courseId}`）。
     *
     * 返回 `{code, msg, data:{course, chapters:[{...,videos:[]}], videos:[]}}`：
     *  - course  课程本体（含 viewCount / avgScore / likeCount 等计数）
     *  - chapters 章节，每章带自己的 videos
     *  - videos  没有归属章节的课时
     *
     * 用 JsonObject 接而不是固定 DTO：后端给的是带 RuoYi 审计字段的完整实体，
     * 手写映射可以只取需要的字段，避免字段增删直接反序列化失败。
     */
    @GET("api/v1/course/{courseId}")
    suspend fun getCourseDetail(
        @Path("courseId") courseId: Long
    ): Response<JsonObject>
}