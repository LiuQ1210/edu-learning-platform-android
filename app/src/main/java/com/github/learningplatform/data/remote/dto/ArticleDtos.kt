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
    /**
     * 文章状态：0草稿 1待审核 2已发布 3已驳回 4已下架。
     * 文档 7.1 未列出该字段，但 7.6「我的发布」按 status 筛选，列表项必然回传，
     * 这里补上并给默认值 2（已发布），避免解析失败。
     */
    val status: Int = 2
)

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