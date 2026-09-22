package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 统一内容项（接口文档 v3.1 增补 1.2 节）。
 *
 * 4.2「根据分类获取内容列表」与 5.1「内容搜索」v3.0 只写了「分页列表」，没有字段定义。
 * v3.1 增补把两者的列表项统一为这个结构：
 *
 *  - 主键字段名固定为 `id`：`type=1` 是 article_id，`type=2` 是 course_id；
 *  - `type` **必须**回传，客户端据此决定跳文章详情还是课程详情；
 *  - 6.1(课程列表) / 7.1(文章列表) 仍用各自的 `courseId` / `articleId`，不要混用。
 */
@Serializable
data class ContentItemDto(
    /** 内容主键：type=1 -> articleId，type=2 -> courseId */
    val id: Long = 0,
    /** 1-文章 2-视频课程 */
    val type: Int = 1,
    val title: String = "",
    val summary: String = "",
    val coverUrl: String = "",
    val categoryId: Long = 0,
    val categoryName: String = "",
    val authorId: Long = 0,
    val authorName: String = "",
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    /** 视频时长（秒），文章为 0 */
    val duration: Int = 0,
    /** 课程平均评分，文章为 0 */
    val score: Double = 0.0,
    val createTime: String = "",
    val publishTime: String = ""
) {
    val isArticle: Boolean get() = type == TARGET_TYPE_ARTICLE
    val isCourse: Boolean get() = type == TARGET_TYPE_COURSE

    companion object {
        const val TARGET_TYPE_ARTICLE = 1
        const val TARGET_TYPE_COURSE = 2
    }
}
