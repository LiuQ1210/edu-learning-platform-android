package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.data.remote.ResourceApi
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 资源下载仓库。
 *
 * 后端目前没有 `GET /resources/download/{resourceId}`（App 端未提供下载接口），
 * 因此这里明确抛「暂未开放」，而不是请求一个必然 404 的地址，
 * 避免把 404 当成网络错误误导排查。
 */
@Singleton
class DownloadRepository @Inject constructor(
    @Suppress("unused") private val resourceApi: ResourceApi
) {

    /**
     * @param onProgress (已下载字节, 总字节)
     */
    suspend fun downloadTo(
        resourceId: Long,
        target: File,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): File {
        throw ApiException(-1, "资源下载暂未开放")
    }
}
