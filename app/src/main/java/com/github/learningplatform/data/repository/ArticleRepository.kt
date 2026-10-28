package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.remote.ArticleApi
import com.github.learningplatform.data.remote.dto.ArticleDetailDto
import com.github.learningplatform.data.remote.dto.ArticleDto
import com.github.learningplatform.data.remote.dto.ArticleTagDto
import com.github.learningplatform.data.remote.dto.CreateArticleRequest
import com.github.learningplatform.data.remote.dto.CreateArticleResponse
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.ReportRequest
import com.github.learningplatform.data.remote.dto.UpdateArticleRequest
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 社区文章仓库。
 *
 * 已与后端对齐：详情 / 我的发布 / 删除（后端 post 系列，字段名是 postId/postTitle）。
 * 后端尚未提供、继续用本地样例：文章列表流、发布、编辑、撤回、标签、举报。
 */
@Singleton
class ArticleRepository @Inject constructor(
    private val articleApi: ArticleApi
) {

    /**
     * 社区文章列表流（对接后端 `/api/v1/post/list`）。
     *
     * 后端返回字段按设计稿裁剪：postId / postTitle / userName / avatar /
     * viewCount / likeCount / favoriteCount / commentCount（**不含 createTime**），
     * 所以列表页的发布时间会是空串。
     *
     * 支持按 categoryId 筛选；排序固定为发布时间倒序，
     * `sort`（最新/最热/精华）后端暂未支持，传了不生效。
     */
    suspend fun getArticles(
        pageNum: Int = 1,
        pageSize: Int = 20,
        categoryId: Long? = null,
        tagId: Long? = null,
        sort: Int? = null
    ): PageData<ArticleDto> {
        val table = articleApi.getPublishedArticles(
            categoryId = categoryId,
            pageNum = pageNum,
            pageSize = pageSize
        ).okTable()
        return PageData(total = table.total, list = table.rows.map { it.toArticle() })
    }

    suspend fun getArticleDetail(articleId: Long): ArticleDetailDto =
        articleApi.getArticleDetail(articleId).okBody().payload().toArticleDetail()

    /** 发布：后端未提供，本地返回成功 */
    suspend fun createArticle(request: CreateArticleRequest): CreateArticleResponse =
        CreateArticleResponse(articleId = 5999L, status = request.publish)

    /** 编辑：后端未提供 */
    suspend fun updateArticle(articleId: Long, request: UpdateArticleRequest) = Unit

    suspend fun deleteArticle(articleId: Long) {
        articleApi.deleteArticle(articleId).okBody()
    }

    /** 我的发布（后端只返回当前登录用户） */
    suspend fun getMyArticles(
        pageNum: Int = 1,
        pageSize: Int = 20,
        status: Int? = null
    ): PageData<ArticleDto> {
        val table = articleApi.getMyArticles(pageNum, pageSize).okTable()
        val list = table.rows.map { it.toArticle() }
        return PageData(total = table.total, list = list)
    }

    /** 撤回为草稿：后端未提供 */
    suspend fun withdrawArticle(articleId: Long) = Unit

    /** 标签：后端未提供，使用本地样例 */
    suspend fun getTags(): List<ArticleTagDto> = DemoData.articleTags

    /** 举报：后端未提供 */
    suspend fun reportArticle(articleId: Long, request: ReportRequest) = Unit
}

private fun JsonObject.toArticle(): ArticleDto = ArticleDto(
    articleId = long("postId"),
    title = str("postTitle"),
    summary = str("summary"),
    coverUrl = str("coverUrl"),
    authorId = long("userId"),
    authorName = str("userName"),
    authorAvatar = str("userAvatar"),
    categoryName = str("categoryName"),
    viewCount = int("viewCount"),
    likeCount = int("likeCount"),
    commentCount = int("commentCount"),
    publishTime = str("createTime"),
    // 后端 status 是字符串："0"=已发布；客户端 0草稿 1待审核 2已发布 3驳回 4下架
    status = if (str("status") == "0") 2 else 2
)

private fun JsonObject.toArticleDetail(): ArticleDetailDto = ArticleDetailDto(
    articleId = long("postId"),
    title = str("postTitle"),
    summary = str("summary"),
    content = str("content"),
    coverUrl = str("coverUrl"),
    authorId = long("userId"),
    authorName = str("userName"),
    authorAvatar = str("userAvatar"),
    categoryId = long("categoryId"),
    categoryName = str("categoryName"),
    viewCount = int("viewCount"),
    likeCount = int("likeCount"),
    commentCount = int("commentCount"),
    favoriteCount = int("favoriteCount"),
    publishTime = str("createTime")
)
