package com.github.learningplatform.data.repository

import android.content.Context
import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.core.network.safeApiCallForUnit
import com.github.learningplatform.data.remote.FileApi
import com.github.learningplatform.data.remote.dto.UploadFileDto
import com.github.learningplatform.data.remote.dto.UploadScene
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 文件上传仓库（接口文档 v3.1 增补 15.1）。
 *
 * 客户端职责（增补 15.3 的落地约定）：
 *  1. 上传前本地压缩：图片长边压到 ≤ 1600px、JPEG 质量 85，
 *     避免把手机原图（5-10 MB）直接推上去；
 *  2. 字段名必须是 `file`；
 *  3. 上传成功只认服务端返回的 `url`，不自己拼 CDN 前缀；
 *  4. 上传与业务写入是两步，调用方拿到 url 后再调业务接口。
 */
@Singleton
class FileRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val fileApi: FileApi
) {

    /** 图片压缩目标：长边上限（px） */
    private val maxImageEdge = 1600

    /** JPEG 压缩质量 */
    private val jpegQuality = 85

    /**
     * 上传文件。
     *
     * @param rawFile 原始文件（相机/相册返回的临时文件）
     * @param scene 业务场景，决定服务端归档目录与大小上限
     * @return 服务端返回的 url；调用方拿到后再调业务接口落库
     * @throws IllegalArgumentException 文件为空或超过该 scene 的大小上限
     */
    suspend fun upload(rawFile: File, scene: UploadScene): UploadFileDto {
        require(rawFile.exists() && rawFile.length() > 0L) { "文件不存在或内容为空" }

        // 图片先压缩再上传；非图片（资源文件）原样上传
        val isImage = rawFile.extension.lowercase() in IMAGE_EXTENSIONS
        val file = if (isImage) compressImage(rawFile) else rawFile

        val limit = scene.maxBytes
        require(file.length() <= limit) {
            "文件超过上限（${limit / 1024 / 1024} MB），请重新选择"
        }

        val part = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody(mimeTypeOf(file).toMediaTypeOrNull())
        )
        val sceneBody = scene.code.toRequestBody("text/plain".toMediaTypeOrNull())

        return safeApiCall { fileApi.upload(part, sceneBody) }
    }

    /** 删除文件（替换头像/封面后清理旧文件） */
    suspend fun delete(fileId: String) {
        if (fileId.isBlank()) return
        safeApiCallForUnit { fileApi.deleteFile(fileId) }
    }

    /**
     * 图片压缩。
     *
     * 用 BitmapFactory 的 inSampleSize 做粗采样，再按长边缩放，
     * 全程在 IO 线程执行；压缩失败时退回原文件，不阻断上传。
     */
    private suspend fun compressImage(source: File): File = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(source.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching source

            // 粗采样：先按 2 的幂次把尺寸降到目标附近，减少内存峰值
            val options = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight)
            }
            val decoded = android.graphics.BitmapFactory.decodeFile(source.absolutePath, options)
                ?: return@runCatching source

            // 精修：按长边缩放到上限
            val longEdge = maxOf(decoded.width, decoded.height)
            val scaled = if (longEdge > maxImageEdge) {
                val ratio = maxImageEdge.toFloat() / longEdge
                android.graphics.Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * ratio).toInt().coerceAtLeast(1),
                    (decoded.height * ratio).toInt().coerceAtLeast(1),
                    true
                ).also { if (it !== decoded) decoded.recycle() }
            } else {
                decoded
            }

            val target = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(target).use { out ->
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, jpegQuality, out)
            }
            if (scaled !== decoded) scaled.recycle() else decoded.recycle()

            // 压缩反而变大（已是小图）时保留原文件
            if (target.length() in 1 until source.length()) target else source
        }.getOrDefault(source)
    }

    private fun calculateInSampleSize(width: Int, height: Int): Int {
        var sample = 1
        var longEdge = maxOf(width, height)
        while (longEdge / 2 >= maxImageEdge) {
            longEdge /= 2
            sample *= 2
        }
        return sample
    }

    private fun mimeTypeOf(file: File): String = when (file.extension.lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "jpg", "jpeg" -> "image/jpeg"
        "pdf" -> "application/pdf"
        "zip" -> "application/zip"
        else -> "application/octet-stream"
    }

    private companion object {
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")
    }
}