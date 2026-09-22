package com.github.learningplatform.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 后端统一响应体
 * 对应接口文档: { code, message, data, timestamp, requestId }
 */
@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null,
    val timestamp: Long = 0L,
    @SerialName("requestId") val requestId: String = ""
) {
    val isSuccess: Boolean get() = code == 0
}
