package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.core.network.double
import com.github.learningplatform.core.network.int
import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.remote.SearchApi
import com.github.learningplatform.data.remote.dto.ContentItemDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.HotWordDto
import com.github.learningplatform.data.remote.dto.PageData
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索仓库。
 *
 * 后端已实现三个分域搜索接口，客户端按内容类型分流：
 *  - `type=1` 文章：GET /api/v1/search/post
 *  - `type=2` 视频课程：GET /api/v1/search/course
 *  - 分类页点进来（有关键词为空 + categoryId）：走课程列表接口按分类浏览
 *
 * 搜索热词后端未提供，仍用本地样例。
 */
@Singleton
class SearchRepository @Inject constructor(
    private val searchApi: SearchApi,
    private val courseRepository: CourseRepository
) {

    /**
     * 搜索结果。
     *
     * 后端三个搜索接口都不支持 categoryId，所以「从分类页进入、还没输入关键词」
     * 的场景直接查课程列表；一旦用户输入关键词，就按关键词全局搜索。
     */
    suspend fun searchPage(
        keyword: String?,
        type: Int,
        categoryId: Long? = null,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): PageData<ContentItemDto> {
        if (categoryId != null && keyword.isNullOrBlank()) {
            val page = courseRepository.getCourses(
                pageNum = pageNum,
                pageSize = pageSize,
                categoryId = categoryId
            )
            return PageData(total = page.total, list = page.list.map { it.toContentItem() })
        }
        return when (type) {
            ContentItemDto.TARGET_TYPE_ARTICLE -> searchPosts(keyword, pageNum, pageSize)
            else -> searchCourses(keyword, pageNum, pageSize)
        }
    }

    private suspend fun searchPosts(
        keyword: String?,
        pageNum: Int,
        pageSize: Int
    ): PageData<ContentItemDto> {
        val table = searchApi.searchPosts(keyword, pageNum, pageSize).okTable()
        return PageData(
            total = table.total,
            list = table.rows.map { it.toContentItem(ContentItemDto.TARGET_TYPE_ARTICLE) }
        )
    }

    private suspend fun searchCourses(
        keyword: String?,
        pageNum: Int,
        pageSize: Int
    ): PageData<ContentItemDto> {
        val response = searchApi.searchCourses(keyword, pageNum, pageSize)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: " + response.code())
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.msg)
        }
        return PageData(total = body.total, list = body.rows.map { it.toContentItem() })
    }

    /** 搜索热词：后端未提供，使用本地样例 */
    suspend fun getHotWords(size: Int = 10): List<HotWordDto> = DemoData.hotWords
}

/**
 * 帖子搜索行 -> 统一内容项。
 *
 * 后端 search/post 返回的是 t_post 表的行（postId / postTitle / userName …），
 * 字段名与课程表不同，这里做一次性映射；缺失字段留空，不让 UI 崩。
 */
private fun JsonObject.toContentItem(type: Int): ContentItemDto {
    val id = long("postId").takeIf { it > 0 }
        ?: long("courseId").takeIf { it > 0 }
        ?: long("articleId").takeIf { it > 0 }
        ?: long("id")
    val publishTime = str("createTime").ifBlank { str("publishTime") }
    return ContentItemDto(
        id = id,
        type = int("type", type).takeIf { it == 1 || it == 2 } ?: type,
        title = str("postTitle").ifBlank { str("courseName") }.ifBlank { str("title") },
        summary = str("summary").ifBlank { str("intro") },
        coverUrl = str("coverUrl"),
        categoryId = long("categoryId"),
        categoryName = str("categoryName"),
        authorId = long("userId").takeIf { it > 0 } ?: long("authorId"),
        authorName = str("userName")
            .ifBlank { str("authorName") }
            .ifBlank { str("instructorName") },
        viewCount = int("viewCount").takeIf { it > 0 } ?: int("studentCount"),
        likeCount = int("likeCount"),
        commentCount = int("commentCount"),
        duration = int("duration"),
        score = double("avgScore"),
        createTime = publishTime,
        publishTime = publishTime
    )
}

/** 课程搜索结果 -> 统一内容项（type 固定 2）。 */
private fun CourseDto.toContentItem(): ContentItemDto = ContentItemDto(
    id = courseId,
    type = ContentItemDto.TARGET_TYPE_COURSE,
    title = title,
    summary = intro,
    coverUrl = coverUrl,
    categoryId = categoryId,
    categoryName = categoryName,
    authorName = instructorName,
    viewCount = viewCount ?: 0,
    likeCount = likeCount,
    commentCount = commentCount,
    duration = duration,
    score = averageScore,
    createTime = createTime,
    publishTime = createTime
)
