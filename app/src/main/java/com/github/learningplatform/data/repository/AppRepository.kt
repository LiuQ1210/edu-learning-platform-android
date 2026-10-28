package com.github.learningplatform.data.repository

import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.local.CacheKeys
import com.github.learningplatform.data.local.CacheStore
import com.github.learningplatform.data.local.CacheTtl
import com.github.learningplatform.data.local.cachedList
import com.github.learningplatform.data.local.cachedObject
import com.github.learningplatform.data.remote.AppApi
import com.github.learningplatform.data.remote.dto.AdSlotDto
import com.github.learningplatform.data.remote.dto.AppConfigDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 广告位与 APP 配置仓库。
 *
 * 后端目前没有 App 端的 /ad-slots 与 /app-config 接口，
 * 按约定先用本地样例渲染（仍走缓存，避免每次重复构造）。
 * 后端补齐后把 DemoData 换回 appApi 调用即可。
 */
@Singleton
class AppRepository @Inject constructor(
    @Suppress("unused") private val appApi: AppApi,
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

    suspend fun getAppConfig(forceRefresh: Boolean = false): AppConfigDto {
        val key = CacheKeys.config(MODULE_SLOT, NAME_CONFIG)
        if (forceRefresh) cache.remove(key)
        return cache.cachedObject(
            key = key,
            ttlMillis = CacheTtl.STATIC
        ) {
            DemoData.appConfig
        }
    }

    suspend fun invalidate() = cache.invalidateModule(MODULE_SLOT)

    private companion object {
        const val MODULE_SLOT = "ad_slots"
        const val NAME_CONFIG = "app_config"
    }
}