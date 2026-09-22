package com.github.learningplatform.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * 资源下载（接口文档 v3.0 12.1）。
 *
 * 返回文件流，支持 Range 断点续传（服务端返回 206）。
 * 限流：单用户 10 次/分钟；错误码 9001 无下载权限 / 9002 资源不存在。
 * 注：下载记录由服务端在成功下载后写入，客户端另有 3.11 查询接口。
 */
interface ResourceApi {

    @Streaming
    @GET("resources/download/{resourceId}")
    suspend fun download(
        @Path("resourceId") resourceId: Long,
        /** 形如 "bytes=1024-"，首次下载传 null */
        @Header("Range") range: String? = null
    ): Response<ResponseBody>
}