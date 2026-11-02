package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.RawTableData
import com.github.learningplatform.data.remote.dto.PublishArticleRequest
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 社区帖子接口（只声明后端**已实现**的端点）。
 *
 * 后端 App 端与帖子相关的接口：
 *  - `POST /api/v1/post/add`     发布文章（普通用户提交后落「待审核」）
 *  - `GET  /api/v1/post/list`    已发布文章列表（社区首页 / 分类页）
 *  - `GET  /api/v1/post/myList`  我的发布（含草稿/待审核/已驳回）
 *  - `GET  /api/v1/post/info`    帖子详情（业务字段在 `data` 下）
 *  - `POST /api/v1/post/remove`  删除
 *
 * 编辑 / 撤回 / 标签 / 举报后端尚未提供。
 */
interface ArticleApi {

    /**
     * 发布文章（JSON 请求体）。
     *
     * 后端只接收 `postTitle / categoryId / content` 三个字段：
     * 作者、状态、计数全部由服务端赋值；普通用户提交后状态为「3 待审核」。
     *
     * @return `{code:0, msg:"提交成功，等待审核", postId:新ID, status:"3"}`
     */
    @POST("api/v1/post/add")
    suspend fun createArticle(
        @Body body: PublishArticleRequest
    ): Response<JsonObject>

    /**
     * 已发布文章列表（社区首页 / 分类页）。
     *
     * @param categoryId 类别ID；不传 = 全部分类
     * @param featured 只看精华：true 只看精华，不传 = 全部
     * @param sortBy 排序：createTime（缺省）/ likeCount
     */
    @GET("api/v1/post/list")
    suspend fun getPublishedArticles(
        @Query("categoryId") categoryId: Long? = null,
        @Query("featured") featured: Boolean? = null,
        @Query("sortBy") sortBy: String? = null,
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
