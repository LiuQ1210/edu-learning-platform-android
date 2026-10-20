package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.ContentItemDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.HotWordListDto
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.TableDataInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** 搜索模块 ApiV1 接口 */
interface SearchApi {

    /** 课程搜索 */
    @GET("api/v1/search/course")
    suspend fun searchCourses(
        @Query("keyword") keyword: String?,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<TableDataInfo<CourseDto>>

    /** 视频搜索 */
    @GET("api/v1/search/video")
    suspend fun searchVideos(
        @Query("keyword") keyword: String?,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<TableDataInfo<Map<String, Any>>>

    /** 帖子搜索 */
    @GET("api/v1/search/post")
    suspend fun searchPosts(
        @Query("keyword") keyword: String?,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<TableDataInfo<Map<String, Any>>>

    /** 旧搜索接口（兼容，后端暂未对接） */
    @GET("content/search")
    suspend fun search(
        @Query("keyword") keyword: String?,
        @Query("type") type: Int,
        @Query("categoryId") categoryId: Long? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiResponse<PageData<ContentItemDto>>>

    @GET("content/search/hot-words")
    suspend fun getHotWords(@Query("size") size: Int = 10): Response<ApiResponse<HotWordListDto>>
}
