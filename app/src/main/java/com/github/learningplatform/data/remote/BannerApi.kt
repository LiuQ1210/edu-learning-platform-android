package com.github.learningplatform.data.remote

import com.github.learningplatform.data.remote.dto.BannerDto
import com.github.learningplatform.data.remote.dto.TableDataInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 首页轮播图（对齐后端 ApiV1BannerController）。
 *
 * `GET /api/v1/banner/list?categoryId=`（不传 = 全部窗口 category_id = 0）
 * 返回 `{code, msg, rows:[...], total}`，行内字段见 [BannerDto]。
 */
interface BannerApi {

    @GET("api/v1/banner/list")
    suspend fun getBanners(
        @Query("categoryId") categoryId: Long? = null
    ): Response<TableDataInfo<BannerDto>>
}
