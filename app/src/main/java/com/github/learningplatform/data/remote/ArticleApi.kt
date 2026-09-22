package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.ArticleDetailDto
import com.github.learningplatform.data.remote.dto.ArticleDto
import com.github.learningplatform.data.remote.dto.ArticleTagDto
import com.github.learningplatform.data.remote.dto.CreateArticleRequest
import com.github.learningplatform.data.remote.dto.CreateArticleResponse
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.ReportRequest
import com.github.learningplatform.data.remote.dto.UpdateArticleRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** 社区文章模块 APP 端（接口文档 v3.0 7.1-7.8、8.12） */
interface ArticleApi {

    /** @param sort 1最新 2最热 3精华 */
    @GET("articles")
    suspend fun getArticles(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("categoryId") categoryId: Long? = null,
        @Query("tagId") tagId: Long? = null,
        @Query("sort") sort: Int? = null
    ): Response<ApiResponse<PageData<ArticleDto>>>

    @GET("articles/{articleId}")
    suspend fun getArticleDetail(
        @Path("articleId") articleId: Long
    ): Response<ApiResponse<ArticleDetailDto>>

    @POST("articles")
    suspend fun createArticle(
        @Body body: CreateArticleRequest
    ): Response<ApiResponse<CreateArticleResponse>>

    @PUT("articles/{articleId}")
    suspend fun updateArticle(
        @Path("articleId") articleId: Long,
        @Body body: UpdateArticleRequest
    ): Response<ApiResponse<Unit>>

    @DELETE("articles/{articleId}")
    suspend fun deleteArticle(@Path("articleId") articleId: Long): Response<ApiResponse<Unit>>

    /** 7.6 我的发布。status: 0草稿 1待审核 2已发布 3驳回 4下架 */
    @GET("user/articles")
    suspend fun getMyArticles(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("status") status: Int? = null
    ): Response<ApiResponse<PageData<ArticleDto>>>

    /** 7.7 撤回为草稿 */
    @POST("articles/{articleId}/withdraw")
    suspend fun withdrawArticle(@Path("articleId") articleId: Long): Response<ApiResponse<Unit>>

    /** 7.8 标签列表（按使用量排序） */
    @GET("article-tags")
    suspend fun getTags(): Response<ApiResponse<List<ArticleTagDto>>>

    /** 8.12 举报文章 */
    @POST("articles/{articleId}/report")
    suspend fun reportArticle(
        @Path("articleId") articleId: Long,
        @Body body: ReportRequest
    ): Response<ApiResponse<Unit>>
}