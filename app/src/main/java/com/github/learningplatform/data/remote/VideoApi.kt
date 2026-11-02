package com.github.learningplatform.data.remote

import com.github.learningplatform.data.remote.dto.ShortVideoDto
import com.github.learningplatform.data.remote.dto.TableDataInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 短视频模块 APP 端 ApiV1 接口
 * 对应后端 learning_short_video 表
 */
interface VideoApi {

    /**
     * 短视频信息流（分页，最新在前）
     */
    @GET("api/v1/video/list")
    suspend fun getVideoList(
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): Response<TableDataInfo<ShortVideoDto>>
}
