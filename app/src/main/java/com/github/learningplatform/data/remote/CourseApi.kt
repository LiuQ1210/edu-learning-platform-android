package com.github.learningplatform.data.remote

import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.TableDataInfo
import retrofit2.Response
import retrofit2.http.GET
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
}