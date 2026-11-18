package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.payload
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.ArticleApi
import com.github.learningplatform.data.remote.TopicApi
import com.github.learningplatform.data.remote.dto.ArticleDetailDto
import com.github.learningplatform.data.remote.dto.ArticleDto
import com.github.learningplatform.data.remote.dto.ArticleTagDto
import com.github.learningplatform.data.remote.dto.CreateArticleRequest
import com.github.learningplatform.data.remote.dto.CreateArticleResponse
import com.github.learningplatform.data.remote.dto.PageData
import com.github.learningplatform.data.remote.dto.PostStatus
import com.github.learningplatform.data.remote.dto.PublishArticleRequest
import com.github.learningplatform.data.remote.dto.ReportRequest
import com.github.learningplatform.data.remote.dto.TopicDetailDto
import com.github.learningplatform.data.remote.dto.UpdateArticleRequest
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 社区文章仓库。
 *
 * 已与后端对齐：文章列表流 / 详情 / 我的发布 / 发布 / 删除（后端 post 系列，字段名是 postId/postTitle）；
 * 话题字典与话题详情走 topic 系列。
 * 后端尚未提供：编辑、撤回、举报。
 */
@Singleton
class ArticleRepository @Inject constructor(
    private val articleApi: ArticleApi,
    private val topicApi: TopicApi
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
        authorId: Long? = null,
        sort: Int? = null
    ): PageData<ArticleDto> {
        // 社区排序 tab（1最新 / 2最热 / 3精华）映射到后端参数：
        //   最新 → sortBy=createTime；最热 → sortBy=likeCount；精华 → featured=true
        val featured = if (sort == 3) true else null
        val sortBy = when (sort) {
            2 -> "likeCount"
            1, 3 -> "createTime"
            else -> null // 不传 = 后端默认 createTime
        }
        val table = articleApi.getPublishedArticles(
            categoryId = categoryId,
            featured = featured,
            sortBy = sortBy,
            userId = authorId,
            topicId = tagId,
            pageNum = pageNum,
            pageSize = pageSize
        ).okTable()
        return PageData(total = table.total, list = table.rows.map { it.toArticle() })
    }

    suspend fun getArticleDetail(articleId: Long): ArticleDetailDto =
        articleApi.getArticleDetail(articleId).okBody().payload().toArticleDetail()

    /**
     * 发布文章（后端 `POST /api/v1/post/add`）。
     *
     * 后端只接收「标题 / 分类 / 正文」，普通用户提交后状态为「待审核」，
     * 返回 `{postId, status:"3"}`（status 是字符串，这里转成 Int）。
     */
    suspend fun createArticle(request: CreateArticleRequest): CreateArticleResponse {
        val body = articleApi.createArticle(
            PublishArticleRequest(
                postTitle = request.title,
                categoryId = request.categoryId.toString(),
                content = request.content,
                topicIds = request.tagIds?.takeIf { it.isNotEmpty() }?.joinToString(",")
            )
        ).okBody().payload()
        return CreateArticleResponse(
            articleId = body.long("postId"),
            status = body.str("status").toIntOrNull() ?: PostStatus.PENDING
        )
    }

    /** 编辑：后端没有 post/edit 接口，明确报错而不是假装成功 */
    suspend fun updateArticle(articleId: Long, request: UpdateArticleRequest) {
        throw ApiException(-1, "后端暂不支持编辑文章")
    }

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

    /** 话题字典（后端 GET /api/v1/topic/list），复用标签 DTO 展示在发帖选择器 */
    suspend fun getTags(): List<ArticleTagDto> {
        val table = topicApi.getTopicList(pageNum = 1, pageSize = 50).okTable()
        return table.rows.map {
            ArticleTagDto(
                tagId = it.long("topicId"),
                name = it.str("topicName"),
                useCount = it.int("postCount")
            )
        }
    }

    /** 话题详情（话题页顶部：名称 + 简介 + 文章数） */
    suspend fun getTopicDetail(topicId: Long): TopicDetailDto {
        val d = topicApi.getTopicDetail(topicId).okBody().payload()
        return TopicDetailDto(
            topicId = d.long("topicId"),
            topicName = d.str("topicName"),
            topicDesc = d.str("topicDesc"),
            postCount = d.int("postCount")
        )
    }

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
    // 后端列表行的头像字段叫 avatar，详情里才是 userAvatar，两者都兜一下
    authorAvatar = str("avatar").ifBlank { str("userAvatar") },
    categoryName = str("categoryName"),
    viewCount = int("viewCount"),
    likeCount = int("likeCount"),
    commentCount = int("commentCount"),
    publishTime = str("createTime"),
    isFeatured = int("isFeatured") == 1,
    // 后端 status 是字符串："0"已发布 "1"草稿 "2"已下架 "3"待审核（见 PostStatus）
    status = str("status").toIntOrNull() ?: PostStatus.PUBLISHED
)

private fun JsonObject.toArticleDetail(): ArticleDetailDto = ArticleDetailDto(
    articleId = long("postId"),
    title = str("postTitle"),
    summary = str("summary"),
    content = str("content"),
    coverUrl = str("coverUrl"),
    authorId = long("userId"),
    authorName = str("userName"),
    authorAvatar = str("avatar").ifBlank { str("userAvatar") },
    categoryId = long("categoryId"),
    categoryName = str("categoryName"),
    viewCount = int("viewCount"),
    likeCount = int("likeCount"),
    commentCount = int("commentCount"),
    favoriteCount = int("favoriteCount"),
    publishTime = str("createTime")
)
