package com.github.learningplatform.core.network

import com.github.learningplatform.core.constants.Constants
import retrofit2.Response

private fun <T> Response<ApiResponse<T>>.requireBody(): ApiResponse<T> {
    if (!isSuccessful) {
        throw ApiException(code(), "HTTP错误: ${code()} ${message()}")
    }
    return body() ?: throw ApiException(-1, "响应体为空")
}

private fun ApiResponse<*>.requireSuccess() {
    if (code != Constants.CODE_SUCCESS) {
        throw ApiException(code, message, requestId)
    }
}

/**
 * 适用于**有返回数据**的接口。
 * 成功但 data 为 null 时抛业务异常，保护调用方不做多余判空。
 */
suspend fun <T : Any> safeApiCall(call: suspend () -> Response<ApiResponse<T>>): T {
    val body = call().requireBody()
    body.requireSuccess()
    return body.data ?: throw ApiException(body.code, "数据为空")
}

/**
 * 适用于**无返回数据**的接口：logout / delete / 状态更新等。
 *
 * 这类接口成功后 data 通常为 null，若走 [safeApiCall] 会被误判为失败。
 */
suspend fun safeApiCallForUnit(call: suspend () -> Response<ApiResponse<Unit>>) {
    val body = call().requireBody()
    body.requireSuccess()
}