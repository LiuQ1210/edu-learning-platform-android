package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.remote.CategoryApi
import com.github.learningplatform.data.remote.dto.CategoryNodeDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 类别仓库（对接后端 `/api/v1/category/list`）。
 *
 * 后端只提供「平铺类别列表」，没有接口文档里的多级分类树，
 * 也没有「按分类取内容列表」接口——内容列表请直接用
 * `/api/v1/course/list?categoryId=` 或 `/api/v1/post/list?categoryId=`。
 *
 * 客户端内部沿用数字 type（1=文章 2=课程），这里转换成后端的 `article` / `course`。
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryApi: CategoryApi
) {

    /**
     * @param type 1-文章分类 2-视频课程分类
     * @return 平铺列表（children 恒为空，UI 只用到 categoryId/categoryName）
     */
    suspend fun getTree(type: Int): List<CategoryNodeDto> {
        val categoryType = if (type == CATEGORY_TYPE_ARTICLE) "article" else "course"
        val rows = categoryApi.getCategories(categoryType).okTable().rows
        return rows.map {
            CategoryNodeDto(
                categoryId = it.long("categoryId"),
                categoryName = it.str("categoryName")
            )
        }
    }

    private companion object {
        const val CATEGORY_TYPE_ARTICLE = 1
    }
}
