package com.github.learningplatform.core.network

/**
 * 业务异常，由统一响应体code非0时抛出
 */
class ApiException(
    val code: Int,
    override val message: String,
    val requestId: String = ""
) : Exception(message)
