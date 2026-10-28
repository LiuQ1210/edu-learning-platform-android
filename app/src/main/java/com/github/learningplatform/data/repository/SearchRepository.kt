package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.remote.SearchApi
import com.github.learningplatform.data.remote.dto.ContentItemDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.HotWordDto
import com.github.learningplatform.data.remote.dto.PageData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 搜索仓库。
 *
 * 已与后端对齐：课程搜索（/api/v1/search/course，TableDataInfo）。
 * 后端尚未提供、继续用本地样例：内容聚合搜索、搜索热词。
 */
@Singleton
class SearchRepository @Inject constructor(
    private val searchApi: SearchApi
) {

    /** 搜索课程（后端已实现） */
    suspend fun searchCourses(
        keyword: String?,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): PageData<CourseDto> {
        val response = searchApi.searchCourses(keyword, pageNum, pageSize)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: " + response.code())
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.msg)
        }
        return PageData(total = body.total, list = body.rows)
    }

    /** @param type 1-文章 2-视频课程（后端未提供聚合搜索，使用本地样例） */
    suspend fun search(
        keyword: String?,
        type: Int,
        categoryId: Long? = null,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): PageData<ContentItemDto> = DemoData.searchResult(keyword, type, pageNum, categoryId)

    /** 原始分页结果（同 search，本地样例） */
    suspend fun searchPage(
        keyword: String?,
        type: Int,
        categoryId: Long? = null,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): PageData<ContentItemDto> = DemoData.searchResult(keyword, type, pageNum, categoryId)

    /** 搜索热词：后端未提供，使用本地样例 */
    suspend fun getHotWords(size: Int = 10): List<HotWordDto> = DemoData.hotWords
}