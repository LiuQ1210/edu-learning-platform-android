package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.preview
import com.github.learningplatform.data.local.CacheKeys
import com.github.learningplatform.data.local.CacheStore
import com.github.learningplatform.data.local.CacheTtl
import com.github.learningplatform.data.local.cachedObject
import com.github.learningplatform.data.remote.CategoryApi
import com.github.learningplatform.data.remote.dto.CategoryNodeDto
import com.github.learningplatform.data.remote.dto.ContentItemDto
import com.github.learningplatform.data.remote.dto.PageData
import javax.inject.Inject
import javax.inject.Singleton

/** 分类仓库（接口文档 v3.0 4.1 / 4.2） */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryApi: CategoryApi,
    private val cache: CacheStore
) {

    /** @param type 1-文章分类 2-视频课程分类 */
    suspend fun getTree(type: Int): List<CategoryNodeDto> {
        // 按接口 4.1 的原始 type 值分派，不引入 UI 层常量
        preview { return DemoData.categoryTree(type) }

        // 分类树是导航骨架，断网时没有它首页连分类都列不出来，
        // 所以用 STATIC 档（6 小时）并把旧数据兜底留给 cachedObject 处理。
        return cache.cachedObject(
            key = CacheKeys.categoryTree(type),
            ttlMillis = CacheTtl.STATIC
        ) {
            safeApiCall { categoryApi.getTree(type) }
        }
    }

    /**
     * 分类下的内容列表（4.2）。
     *
     * @param forceRefresh 下拉刷新传 true，跳过缓存强制回源
     */
    suspend fun getContentList(
        categoryId: Long,
        type: Int,
        pageNum: Int = 1,
        pageSize: Int = 20,
        sort: Int? = null,
        forceRefresh: Boolean = false
    ): PageData<ContentItemDto> {
        preview { return DemoData.searchResult(null, type, pageNum) }

        // 只缓存第 1 页：翻页数据命中率低，缓存收益不抵存储与失效成本
        if (pageNum != 1) {
            return safeApiCall {
                categoryApi.getContentList(categoryId, type, pageNum, pageSize, sort)
            }
        }

        if (forceRefresh) cache.invalidateModule(MODULE)

        val key = CacheKeys.page(
            module = MODULE,
            "categoryId" to categoryId,
            "type" to type,
            "size" to pageSize,
            "sort" to sort
        )

        /*
         * 缓存整个 PageData 而不是只缓存 list。
         *
         * 只缓存 list 会漏掉 total，而调用方用 `已加载条数 < total` 判断还有没有下一页
         * （见 HomeViewModel.loadCourses）。total 缺失或退化成 list.size 时，
         * 缓存命中一次就会让 hasMore 变成 false，**上拉加载直接失效**。
         * 这个 bug 不报错，只是列表加载不出来，属于最难查的一类。
         */
        return cache.cachedObject(
            key = key,
            ttlMillis = CacheTtl.MEDIUM
        ) {
            safeApiCall {
                categoryApi.getContentList(categoryId, type, pageNum, pageSize, sort)
            }
        }
    }

    /** 清掉内容列表缓存（后台改了内容或设置页「清除缓存」用） */
    suspend fun invalidateContentList() = cache.invalidateModule(MODULE)

    /** 失效分类树 */
    suspend fun invalidateTree() = cache.invalidateModule(MODULE_TREE)

    private companion object {
        const val MODULE = "category_content"
        const val MODULE_TREE = "category_tree"
    }
}
