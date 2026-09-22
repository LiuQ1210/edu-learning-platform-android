package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.ContentItemDto
import com.github.learningplatform.data.remote.dto.HotWordListDto
import com.github.learningplatform.data.remote.dto.PageData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** 搜索模块 APP 端（接口文档 v3.0 5.1 / 5.2，均匿名可访问） */
interface SearchApi {

    /** @param type 1-文章 2-视频课程 */
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