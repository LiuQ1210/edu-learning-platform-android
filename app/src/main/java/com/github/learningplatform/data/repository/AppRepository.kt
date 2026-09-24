package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.preview
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
 * 广告位与 APP 配置仓库（接口文档 13.1 / 14.1）。
 *
 * 两个接口都是「App 启动时拉一次、之后基本不变」的类型，缓存收益最高：
 * 断网时没有它们会导致广告位空、社区审核开关取不到默认值，
 * 而这两者都不该让用户看到错误页。
 */
@Singleton
class AppRepository @Inject constructor(
    private val appApi: AppApi,
    private val cache: CacheStore
) {

    /**
     * 广告位配置。
     *
     * 按 `scene` 分键：同一份接口在不同场景（启动页、社区首页）返回不同 slot。
     * 不分键会让后请求的场景覆盖前一个，表现是「首页广告位显示了社区的场景」。
     */
    suspend fun getAdSlots(scene: String? = null, forceRefresh: Boolean = false): List<AdSlotDto> {
        preview { return DemoData.adSlots }

        val key = CacheKeys.page(MODULE_SLOT, "scene" to scene)
        if (forceRefresh) cache.remove(key)

        // 用 STATIC 档（6 小时）：广告位是运营配置，变更频率极低
        return cache.cachedList(
            key = key,
            ttlMillis = CacheTtl.STATIC
        ) {
            safeApiCall { appApi.getAdSlots(scene) }
        }
    }

    /**
     * APP 全局配置（标题、免费试看时长、社区审核开关等）。
     *
     * 也走缓存：启动时若拿不到配置，界面标题会退化成空字符串。
     * 用缓存兜底比让调用方处理 null 更省事，也避免各处重复判空。
     */
    suspend fun getAppConfig(forceRefresh: Boolean = false): AppConfigDto {
        preview { return DemoData.appConfig }

        val key = CacheKeys.config(MODULE_SLOT, NAME_CONFIG)
        if (forceRefresh) cache.remove(key)

        return cache.cachedObject(
            key = key,
            ttlMillis = CacheTtl.STATIC
        ) {
            safeApiCall { appApi.getAppConfig() }
        }
    }

    /**
     * 清掉广告位与配置缓存。
     *
     * 两项共用 [MODULE_SLOT] 作模块前缀，所以一次调用两者都清
     * （键分别是 `v1:ad_slots:...` 和 `v1:config:ad_slots:...`）。
     * 这正是 [CacheKeys] 要求键以模块名开头的原因 —— 否则这里会漏清。
     */
    suspend fun invalidate() = cache.invalidateModule(MODULE_SLOT)

    private companion object {
        const val MODULE_SLOT = "ad_slots"
        const val NAME_CONFIG = "app_config"
    }
}
