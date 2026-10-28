package com.github.learningplatform.data.remote

import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * 文件上传接口（对齐后端 ApiV1UploadController）。
 *
 * 后端只有 `POST /api/v1/upload`，参数名必须是 `file`，
 * 返回平铺字段：`{ code, msg, url, fileName }`。
 *
 * 注意：后端**没有** scene 参数，也没有删除文件的接口（`files/{fileId}` 未实现）。
 */
interface FileApi {

    /** 上传文件，返回平铺的 url / fileName */
    @Multipart
    @POST("api/v1/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part
    ): Response<JsonObject>
}
