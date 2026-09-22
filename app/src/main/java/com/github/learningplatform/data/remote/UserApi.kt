package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.BlogHomeDto
import com.github.learningplatform.data.remote.dto.DownloadRecordDto
import com.github.learningplatform.data.remote.dto.JoinedResultDto
import com.github.learningplatform.data.remote.dto.MyCourseDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.SignCalendarDto
import com.github.learningplatform.data.remote.dto.UpdateProfileRequest
import com.github.learningplatform.data.remote.dto.UserProfileDto
import com.github.learningplatform.data.remote.dto.UserTargetItemDto
import com.github.learningplatform.data.remote.dto.ViewHistoryItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 用户中心模块（接口文档 v3.0 第三章，12 个接口）。
 * 除 blog-home 外均需登录。
 */
interface UserApi {

    // 3.1 / 3.2 个人资料
    @GET("user/profile")
    suspend fun getProfile(): Response<ApiResponse<UserProfileDto>>

    @PUT("user/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Response<ApiResponse<Unit>>

    /** 3.3 他人博客主页（公开） */
    @GET("user/blog-home/{userId}")
    suspend fun getBlogHome(
        @Path("userId") userId: Long,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<BlogHomeDto>>

    // 3.4 - 3.6 我的课程
    @GET("user/courses")
    suspend fun getMyCourses(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("keyword") keyword: String? = null
    ): Response<ApiResponse<PageData<MyCourseDto>>>

    @POST("user/courses/{courseId}")
    suspend fun joinCourse(@Path("courseId") courseId: Long): Response<ApiResponse<JoinedResultDto>>

    @DELETE("user/courses/{courseId}")
    suspend fun quitCourse(@Path("courseId") courseId: Long): Response<ApiResponse<Unit>>

    // 3.7 / 3.8 收藏与点赞
    @GET("user/favorites")
    suspend fun getFavorites(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("targetType") targetType: Int? = null
    ): Response<ApiResponse<PageData<UserTargetItemDto>>>

    @GET("user/likes")
    suspend fun getLikes(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("targetType") targetType: Int? = null
    ): Response<ApiResponse<PageData<UserTargetItemDto>>>

    // 3.9 / 3.10 浏览历史
    @GET("user/view-history")
    suspend fun getViewHistory(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("targetType") targetType: Int? = null
    ): Response<ApiResponse<PageData<ViewHistoryItemDto>>>

    /** 不传 targetType 则清空全部 */
    @DELETE("user/view-history")
    suspend fun clearViewHistory(
        @Query("targetType") targetType: Int? = null
    ): Response<ApiResponse<Unit>>

    // 3.11 下载记录
    @GET("user/download-records")
    suspend fun getDownloadRecords(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("resourceType") resourceType: Int? = null
    ): Response<ApiResponse<PageData<DownloadRecordDto>>>

    // 3.12 签到日历
    @GET("user/sign-calendar")
    suspend fun getSignCalendar(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<ApiResponse<SignCalendarDto>>
}