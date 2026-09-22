package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.CategoryNodeDto
import com.github.learningplatform.data.remote.dto.ContentItemDto
import com.github.learningplatform.data.remote.dto.PageData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** 分类模块 APP 端（接口文档 v3.0 4.1 / 4.2，均匿名可访问） */
interface CategoryApi {

    /** @param type 1-文章分类 2-视频课程分类 */
    @GET("category/tree")
    suspend fun getTree(@Query("type") type: Int): Response<ApiResponse<List<CategoryNodeDto>>>

    /** @param sort 选填，1最新 2最热 */
    @GET("category/content-list")
    suspend fun getContentList(
        @Query("categoryId") categoryId: Long,
        @Query("type") type: Int,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("sort") sort: Int? = null
    ): Response<ApiResponse<PageData<ContentItemDto>>>
}