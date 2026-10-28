package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.RawTableData
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 社区帖子接口（只声明后端**已实现**的端点）。
 *
 * 后端 App 端与帖子相关的接口：
 *  - `GET  /api/v1/search/post`  已发布帖子列表（空 keyword = 全部，按点赞数倒序）
 *  - `GET  /api/v1/post/myList`  我的发布
 *  - `GET  /api/v1/post/info`    帖子详情（业务字段在 `data` 下）
 *  - `POST /api/v1/post/remove`  删除
 *
 * 社区流用 `/api/v1/search/post`：后端没有独立的 `/api/v1/post/list`，
 * 但该搜索接口在 keyword 为空时「不过滤、只返回已发布、按点赞数倒序」，
 * 语义上就是社区列表（见 ApiV1SearchController 的类注释）。
 *
 * 发布 / 编辑 / 撤回 / 标签 / 举报 后端尚未提供，Repository 继续用本地样例。
 */
interface ArticleApi {

    /**
     * 已发布文章列表（社区首页 / 分类页）。
     *
     * @param categoryId 类别ID；不传 = 全部分类
     */
    @GET("api/v1/post/list")
    suspend fun getPublishedArticles(
        @Query("categoryId") categoryId: Long? = null,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<RawTableData>

    /** 帖子详情，注意后端 `AjaxResult.success(detail)` 是 **data 包裹** */
    @GET("api/v1/post/info")
    suspend fun getArticleDetail(
        @Query("postId") postId: Long
    ): Response<JsonObject>

    /** 我的发布（后端按登录用户过滤） */
    @GET("api/v1/post/myList")
    suspend fun getMyArticles(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<RawTableData>

    /** 删除帖子 */
    @POST("api/v1/post/remove")
    suspend fun deleteArticle(
        @Query("postId") postId: Long
    ): Response<JsonObject>
}
