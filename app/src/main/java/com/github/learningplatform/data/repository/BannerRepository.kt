package com.github.learningplatform.data.repository

import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.remote.BannerApi
import com.github.learningplatform.data.remote.dto.BannerDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 首页轮播图仓库。
 *
 * 后端 `/api/v1/banner/list` 只读，按 `categoryId` 取对应窗口（不传 = 全部）。
 * 失败时由调用方决定是否回落到「课程前 5 条」的旧展示。
 */
@Singleton
class BannerRepository @Inject constructor(
    private val bannerApi: BannerApi
) {

    suspend fun getBanners(categoryId: Long? = null): List<BannerDto> {
        val response = bannerApi.getBanners(categoryId)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP错误: ${response.code()} ${response.message()}")
        }
        val body = response.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != Constants.CODE_SUCCESS) {
            throw ApiException(body.code, body.msg)
        }
        // 后端已按 order_num 排序，这里再兜一次底，避免乱序
        return body.rows.sortedBy { it.orderNum }
    }
}
