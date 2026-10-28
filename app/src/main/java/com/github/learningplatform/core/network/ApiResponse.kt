package com.github.learningplatform.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 后端统一响应体。
 *
 * 注意：后端（RuoYi）实际返回的文案字段名是 `msg`，不是接口文档写的 `message`。
 * 这里用 [SerialName] 把两者对齐，并给 `message` 默认值 —— 少了默认值，
 * 后端不返回该字段时 kotlinx.serialization 会直接抛 MissingFieldException。
 *
 * 只用于 `{ code, msg, data: {...} }` 这种**带 data 包裹**的接口；
 * 平铺字段的接口请用 [okBody] + [payload]。
 */
@Serializable
data class ApiResponse<T>(
    val code: Int = 0,
    @SerialName("msg") val message: String = "",
    val data: T? = null,
    val timestamp: Long = 0L,
    @SerialName("requestId") val requestId: String = ""
) {
    val isSuccess: Boolean get() = code == 0
}
