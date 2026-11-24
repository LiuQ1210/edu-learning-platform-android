package com.github.learningplatform.data.repository

import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.local.CacheKeys
import com.github.learningplatform.data.local.CacheStore
import com.github.learningplatform.data.local.CacheTtl
import com.github.learningplatform.data.local.cachedList
import com.github.learningplatform.data.remote.dto.AdSlotDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 广告位仓库。
 *
 * 后端没有 App 端的 `/ad-slots` 接口，广告位用本地样例渲染
 * （走缓存，避免每次重复构造）。以后后端补齐接口，在这里换成网络请求即可。
 */
@Singleton
class AppRepository @Inject constructor(
    private val cache: CacheStore
) {

    suspend fun getAdSlots(scene: String? = null, forceRefresh: Boolean = false): List<AdSlotDto> {
        val key = CacheKeys.page(MODULE_SLOT, "scene" to scene)
        if (forceRefresh) cache.remove(key)
        return cache.cachedList(
            key = key,
            ttlMillis = CacheTtl.STATIC
        ) {
            DemoData.adSlots
        }
    }

    suspend fun invalidate() = cache.invalidateModule(MODULE_SLOT)

    private companion object {
        const val MODULE_SLOT = "ad_slots"
    }
}