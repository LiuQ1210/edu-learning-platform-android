package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.CourseDetailDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.PlayInfoDto
import com.github.learningplatform.data.remote.dto.ProgressSyncRequest
import com.github.learningplatform.data.remote.dto.RatingDto
import com.github.learningplatform.data.remote.dto.RatingRequest
import com.github.learningplatform.data.remote.dto.RatingResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 课程视频模块 APP 端（接口文档 v3.0 6.1-6.6）。
 * 列表/详情/评分列表匿名可访问；播放凭证与进度上报需登录。
 */
interface CourseApi {

    /** @param videoType 1短视频 2长视频；@param sort 1最新 2最热 3评分 */
    @GET("courses")
    suspend fun getCourses(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("categoryId") categoryId: Long? = null,
        @Query("videoType") videoType: Int? = null,
        @Query("sort") sort: Int? = null
    ): Response<ApiResponse<PageData<CourseDto>>>

    @GET("courses/{courseId}")
    suspend fun getCourseDetail(
        @Path("courseId") courseId: Long
    ): Response<ApiResponse<CourseDetailDto>>

    /** 获取播放凭证；短视频不传 lessonId */
    @GET("courses/{courseId}/play-info")
    suspend fun getPlayInfo(
        @Path("courseId") courseId: Long,
        @Query("lessonId") lessonId: Long? = null
    ): Response<ApiResponse<PlayInfoDto>>

    /** 播放进度心跳（高频，后端走 MQ 异步落库） */
    @POST("courses/progress/sync")
    suspend fun syncProgress(
        @Body body: ProgressSyncRequest
    ): Response<ApiResponse<Unit>>

    /** 提交或修改评分（幂等，覆盖更新） */
    @POST("courses/{courseId}/rating")
    suspend fun rateCourse(
        @Path("courseId") courseId: Long,
        @Body body: RatingRequest
    ): Response<ApiResponse<RatingResultDto>>

    @GET("courses/{courseId}/ratings")
    suspend fun getRatings(
        @Path("courseId") courseId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<PageData<RatingDto>>>
}