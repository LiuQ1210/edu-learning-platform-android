package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.UploadFileDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * 文件上传模块（接口文档 v3.1 增补第十五章）。
 *
 * v3.0 只有资源下载（12.1），没有上传，导致拍照签到（9.2 的 photoUrl）、
 * 文章封面（7.3 的 coverUrl）、笔记插图都缺入口。v3.1 增补定义：
 *
 *  15.1 `POST /api/v1/files/upload`   —— multipart，字段 file + scene
 *  15.2 `DELETE /api/v1/files/{fileId}` —— 替换头像/封面后清理旧文件
 *
 * 上传与业务写入是两步：先拿 url，再调对应业务接口把 url 带上。
 */
interface FileApi {

    /**
     * 上传文件。
     *
     * @param file `MultipartBody.Part.createFormData("file", fileName, requestBody)`，
     *             字段名必须是 `file`
     * @param scene 见 [com.github.learningplatform.data.remote.dto.UploadScene]
     */
    @Multipart
    @POST("files/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Part("scene") scene: okhttp3.RequestBody
    ): Response<ApiResponse<UploadFileDto>>

    /** 删除文件（仅本人上传的文件或管理员） */
    @DELETE("files/{fileId}")
    suspend fun deleteFile(@Path("fileId") fileId: String): Response<ApiResponse<Unit>>
}
