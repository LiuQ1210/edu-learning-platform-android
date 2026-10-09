package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.demo.contentPreviewUnit
import com.github.learningplatform.data.remote.ResourceApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 资源下载仓库（接口文档 12.1）。
 *
 * 断点续传策略：若目标文件已存在，则带 `Range: bytes=<已有长度>-` 续传；
 * 服务端返回 206 时追加写入，返回 200（不支持 Range）时从头覆盖。
 */
@Singleton
class DownloadRepository @Inject constructor(
    private val resourceApi: ResourceApi
) {

    /**
     * @param onProgress (已下载字节, 总字节)；总字节为 -1 表示服务端未返回长度
     */
    suspend fun downloadTo(
        resourceId: Long,
        target: File,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): File {
        contentPreviewUnit { return target }
        return withContext(Dispatchers.IO) {
            val existingLength = if (target.exists()) target.length() else 0L
            val rangeHeader = if (existingLength > 0) "bytes=$existingLength-" else null

            val response = resourceApi.download(resourceId, rangeHeader)
            if (!response.isSuccessful) {
                throw ApiException(response.code(), "下载失败（HTTP ${response.code()}）")
            }
            val body = response.body() ?: throw ApiException(-1, "下载响应体为空")

            // 206 表示服务端接受续传，追加写入；否则从头写
            val append = existingLength > 0 && response.code() == 206
            var downloaded = if (append) existingLength else 0L
            val contentLength = body.contentLength()
            val total = if (contentLength > 0) contentLength + downloaded else -1L

            target.parentFile?.mkdirs()
            body.byteStream().use { input ->
                FileOutputStream(target, append).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                    output.flush()
                }
            }
            target
        }
    }
}
