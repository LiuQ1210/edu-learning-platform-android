package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.contentPreview
import com.github.learningplatform.data.local.CacheDao
import com.github.learningplatform.data.local.CacheEntry
import com.github.learningplatform.data.local.CacheKeys
import com.github.learningplatform.data.local.CacheTtl
import com.github.learningplatform.data.remote.CategoryApi
import com.github.learningplatform.data.remote.dto.CategoryNodeDto
import com.github.learningplatform.data.remote.dto.ContentItemDto
import com.github.learningplatform.data.remote.dto.PageData
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** 分类仓库（接口文档 v3.0 4.1 / 4.2） */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryApi: CategoryApi,
    private val cacheDao: CacheDao
) {

    /**
     * 【任务②：定缓存键规则与 TTL；分类树接 Room 缓存】
     *
     * 分类树缓存策略（读缓存优先 / Cache-Aside）：
     *  1. 先读 cache_kv 里 key = category_tree:type={type} 的条目；
     *  2. 命中且未过期（TTL = CacheTtl.CATEGORY_TREE，默认 24h）→ 反序列化直接返回，
     *     零网络请求、断网可用；
     *  3. 未命中 / 已过期 / 反序列化失败 → 回源（真实接口 4.1），成功后序列化写缓存；
     *  4. 写缓存失败（磁盘/IO 异常）不阻断主流程，下次继续回源。
     *
     * 注意：UI_PREVIEW=true（编译期开关）时仍走 DemoData 短路、不读不写缓存，
     * 避免把样例假数据写进生产缓存造成污染；缓存仅对真实接口生效。
     *
     * @param type 1-文章分类 2-视频课程分类
     */
    suspend fun getTree(type: Int): List<CategoryNodeDto> {
        // 按接口 4.1 的原始 type 值分派，不引入 UI 层常量
        contentPreview { return DemoData.categoryTree(type) }

        val key = CacheKeys.categoryTree(type)

        // 1) 读缓存：命中且新鲜直接返回
        cacheDao.getEntry(key)?.let { entry ->
            val fresh = System.currentTimeMillis() - entry.updatedAt <= CacheTtl.CATEGORY_TREE
            if (fresh) {
                runCatching { json.decodeFromString<List<CategoryNodeDto>>(entry.value) }
                    .getOrNull()
                    ?.let { return it }
            }
        }

        // 2) 回源：真实接口
        val tree = safeApiCall { categoryApi.getTree(type) }

        // 3) 写缓存（失败不阻断主流程）
        runCatching {
            cacheDao.put(
                CacheEntry(
                    key = key,
                    value = json.encodeToString(tree),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return tree
    }

    suspend fun getContentList(
        categoryId: Long,
        type: Int,
        pageNum: Int = 1,
        pageSize: Int = 20,
        sort: Int? = null
    ): PageData<ContentItemDto> {
        contentPreview { return DemoData.searchResult(null, type, pageNum) }
        return safeApiCall { categoryApi.getContentList(categoryId, type, pageNum, pageSize, sort) }
    }

    private companion object {
        /** 缓存 JSON 序列化：与接口 DTO 同一套 kotlinx.serialization */
        private val json = Json { ignoreUnknownKeys = true }
    }
}
