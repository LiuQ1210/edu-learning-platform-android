package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.preview
import com.github.learningplatform.data.remote.CategoryApi
import com.github.learningplatform.data.remote.dto.CategoryNodeDto
import com.github.learningplatform.data.remote.dto.ContentItemDto
import com.github.learningplatform.data.remote.dto.PageData
import javax.inject.Inject
import javax.inject.Singleton

/** 分类仓库（接口文档 v3.0 4.1 / 4.2） */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryApi: CategoryApi
) {

    /** @param type 1-文章分类 2-视频课程分类 */
    suspend fun getTree(type: Int): List<CategoryNodeDto> {
        // 按接口 4.1 的原始 type 值分派，不引入 UI 层常量
        preview { return DemoData.categoryTree(type) }
        return safeApiCall { categoryApi.getTree(type) }
    }

    suspend fun getContentList(
        categoryId: Long,
        type: Int,
        pageNum: Int = 1,
        pageSize: Int = 20,
        sort: Int? = null
    ): PageData<ContentItemDto> {
        preview { return DemoData.searchResult(null, type, pageNum) }
        return safeApiCall { categoryApi.getContentList(categoryId, type, pageNum, pageSize, sort) }
    }
}
