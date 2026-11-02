package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

// ---------------------- 7. 社区文章模块 ----------------------

/** 7.1 文章列表项 */
@Serializable
data class ArticleDto(
    val articleId: Long,
    val title: String = "",
    val summary: String = "",
    val coverUrl: String = "",
    val authorId: Long = 0,
    val authorName: String = "",
    val authorAvatar: String = "",
    val categoryName: String = "",
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val publishTime: String = "",
    /** 是否精华（后端 `isFeatured`：1/0，「精华」tab 就是 featured=true 过滤出来的） */
    val isFeatured: Boolean = false,
    /**
     * 文章状态，取值见 [PostStatus]：
     * 0 已发布 / 1 草稿 / 2 已下架 / 3 待审核。
     *
     * 注意：这里以前按「0草稿 1待审核 2已发布 3已驳回 4已下架」写过，与后端定义完全错位，
     * 已按 `Post.java` 的 STATUS_* 常量修正。默认值取 0（已发布），因为 `/post/list`
     * 只会返回已发布文章；`/post/myList` 会带真实状态。
     */
    val status: Int = PostStatus.PUBLISHED
)

/** 后端 Post.java 的文章状态常量 */
object PostStatus {
    const val PUBLISHED = 0
    const val DRAFT = 1
    const val OFFLINE = 2
    const val PENDING = 3
}

/** 7.2 文章详情 */
@Serializable
data class ArticleDetailDto(
    val articleId: Long,
    val title: String = "",
    val summary: String = "",
    /** 富文本 HTML */
    val content: String = "",
    val coverUrl: String = "",
    val authorId: Long = 0,
    val authorName: String = "",
    val authorAvatar: String = "",
    val categoryId: Long = 0,
    val categoryName: String = "",
    val tags: List<ArticleTagDto> = emptyList(),
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val favoriteCount: Int = 0,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val publishTime: String = ""
)

/** 7.8 标签 */
@Serializable
data class ArticleTagDto(
    val tagId: Long = 0,
    val name: String = "",
    val useCount: Int = 0
)

/** 7.3 发布文章 */
@Serializable
data class CreateArticleRequest(
    val title: String,
    val summary: String? = null,
    val content: String,
    val coverUrl: String? = null,
    val categoryId: Long,
    val tagIds: List<Long>? = null,
    /** 0-存草稿 1-提交发布（默认1） */
    val publish: Int = 1
)

@Serializable
data class CreateArticleResponse(val articleId: Long, val status: Int)

/**
 * 发布文章的请求体（后端 `POST /api/v1/post/add`，JSON）。
 *
 * 后端只接收三个字段：作者、状态、各类计数一律由服务端赋值；
 * 普通用户提交后状态固定为「待审核」，所以这里没有 publish/status 参数。
 */
@Serializable
data class PublishArticleRequest(
    /** 后端字段名是 postTitle */
    val postTitle: String,
    /** 后端用 Convert.toLong 解析，传字符串可避免类型不匹配 */
    val categoryId: String,
    /** 正文（后端按 Markdown/HTML 原样存，长度上限 65535） */
    val content: String
)

/** 7.4 编辑文章 */
@Serializable
data class UpdateArticleRequest(
    val title: String? = null,
    val summary: String? = null,
    val content: String? = null,
    val coverUrl: String? = null,
    val categoryId: Long? = null,
    val tagIds: List<Long>? = null,
    val publish: Int? = null
)

/** 8.12 举报（文章 / 评论共用） */
@Serializable
data class ReportRequest(
    val reason: String,
    val description: String? = null
)