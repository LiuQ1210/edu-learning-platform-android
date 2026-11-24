package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.long
import com.github.learningplatform.core.network.okTable
import com.github.learningplatform.core.network.str
import com.github.learningplatform.data.local.CacheKeys
import com.github.learningplatform.data.local.CacheStore
import com.github.learningplatform.data.local.CacheTtl
import com.github.learningplatform.data.local.cachedList
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
    private val categoryApi: CategoryApi,
    private val cache: CacheStore
) {

    /**
     * @param type 1-文章分类 2-视频课程分类
     * @param forceRefresh 下拉刷新等「用户明确要最新」的场景传 true：先失效缓存再回源
     * @return 平铺列表（children 恒为空，UI 只用到 categoryId/categoryName）
     *
     * 走 Room 缓存（TTL 24h）：分类树几乎不变，没必要每次进页面都等一次网络往返。
     * 之前这里直连接口，冷启动时 tab 只能先渲染「全部」，等请求回来才补齐其他分类，
     * 表现为「分类一个个慢慢长出来」。
     */
    suspend fun getTree(type: Int, forceRefresh: Boolean = false): List<CategoryNodeDto> {
        val categoryType = if (type == CATEGORY_TYPE_ARTICLE) "article" else "course"
        val key = CacheKeys.categoryTree(type)
        if (forceRefresh) {
            // 下拉刷新是「我要最新数据」的明确动作，必须先丢掉旧条目
            cache.remove(key)
        }
        return cache.cachedList<CategoryNodeDto>(
            key = key,
            ttlMillis = CacheTtl.CATEGORY_TREE
        ) {
            categoryApi.getCategories(categoryType).okTable().rows.map {
                CategoryNodeDto(
                    categoryId = it.long("categoryId"),
                    categoryName = it.str("categoryName")
                )
            }
        }
    }

    private companion object {
        const val CATEGORY_TYPE_ARTICLE = 1
    }
}
