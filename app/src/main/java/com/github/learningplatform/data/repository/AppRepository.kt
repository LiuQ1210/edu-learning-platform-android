package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.data.demo.DemoData
import com.github.learningplatform.data.demo.contentPreview
import com.github.learningplatform.data.remote.AppApi
import com.github.learningplatform.data.remote.dto.AdSlotDto
import com.github.learningplatform.data.remote.dto.AppConfigDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 广告位与 APP 配置仓库（接口文档 13.1 / 14.1）。
 *
 * 建议在 App 启动时拉取并缓存：广告位用于按 slotCode 取 takuSlotId，
 * 配置用于 APP 标题、免费试看时长、社区审核开关等。
 */
@Singleton
class AppRepository @Inject constructor(
    private val appApi: AppApi
) {

    suspend fun getAdSlots(scene: String? = null): List<AdSlotDto> {
        contentPreview { return DemoData.adSlots }
        return safeApiCall { appApi.getAdSlots(scene) }
    }

    suspend fun getAppConfig(): AppConfigDto {
        contentPreview { return DemoData.appConfig }
        return safeApiCall { appApi.getAppConfig() }
    }
}
