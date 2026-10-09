package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.contentPreview
import com.github.learningplatform.data.remote.SearchApi
import com.github.learningplatform.data.remote.dto.ContentItemDto
import com.github.learningplatform.data.remote.dto.HotWordDto
import com.github.learningplatform.data.remote.dto.PageData
import javax.inject.Inject
import javax.inject.Singleton

/** 搜索仓库（接口文档 v3.0 5.1 / 5.2） */
@Singleton
class SearchRepository @Inject constructor(
    private val searchApi: SearchApi
) {

    /** @param type 1-文章 2-视频课程 */
    suspend fun search(
        keyword: String?,
        type: Int,
        categoryId: Long? = null,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): PageData<ContentItemDto> {
        contentPreview { return DemoData.searchResult(keyword, type, pageNum) }
        return safeApiCall {
            searchApi.search(keyword, type, categoryId, pageNum, pageSize)
        }
    }

    /**
     * 原始分页结果。
     *
     * 搜索结果与「分类详情」共用 ContentItemDto（id 为课程ID 或 文章ID），
     * 因此上层需要拿到完整 PageData 才能做分页累加与总数展示。
     */
    suspend fun searchPage(
        keyword: String?,
        type: Int,
        categoryId: Long? = null,
        pageNum: Int = 1,
        pageSize: Int = 20
    ): PageData<ContentItemDto> {
        contentPreview { return DemoData.searchResult(keyword, type, pageNum) }
        return safeApiCall {
            searchApi.search(keyword, type, categoryId, pageNum, pageSize)
        }
    }

    suspend fun getHotWords(size: Int = 10): List<HotWordDto> {
        contentPreview { return DemoData.hotWords }
        return safeApiCall { searchApi.getHotWords(size) }.hotWordList
    }
}
