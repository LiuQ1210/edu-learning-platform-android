package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.contentPreview
import com.github.learningplatform.data.remote.SearchApi
import com.github.learningplatform.data.remote.dto.ContentItemDto
import com.github.learningplatform.data.remote.dto.CourseDto
import com.github.learningplatform.data.remote.dto.HotWordDto
import com.github.learningplatform.data.remote.dto.PageData
import javax.inject.Inject
import javax.inject.Singleton

/** 搜索仓库（对接后端 ApiV1 接口） */
@Singleton
class SearchRepository @Inject constructor(
    private val searchApi: SearchApi
) {

    /** 搜索课程 */
    suspend fun searchCourses(
        keyword: String?,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): PageData<CourseDto> {
        val response = searchApi.searchCourses(keyword, pageNum, pageSize)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) {
            throw ApiException(body.code, body.msg)
        }
        return PageData(total = body.total, list = body.rows)
    }

    /** @param type 1-文章 2-视频课程 */
    suspend fun search(
        keyword: String?,
        type: Int,
        categoryId: Long? = null,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): PageData<ContentItemDto> {
        contentPreview { return DemoData.searchResult(keyword, type, pageNum, categoryId) }
        return safeApiCall {
            searchApi.search(keyword, type, categoryId, pageNum, pageSize)
        }
    }

    /**
     * 原始分页结果。
     */
    suspend fun searchPage(
        keyword: String?,
        type: Int,
        categoryId: Long? = null,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): PageData<ContentItemDto> {
        contentPreview { return DemoData.searchResult(keyword, type, pageNum, categoryId) }
        return safeApiCall {
            searchApi.search(keyword, type, categoryId, pageNum, pageSize)
        }
    }

    suspend fun getHotWords(size: Int = 10): List<HotWordDto> {
        contentPreview { return DemoData.hotWords }
        return safeApiCall { searchApi.getHotWords(size) }.hotWordList
    }
}
