package com.github.learningplatform.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 后端 RuoYi 的表格分页响应：`{ code, msg, rows, total }`。
 *
 * 为什么 rows 用 [JsonObject] 而不是 Map<String, Any>：
 * kotlinx.serialization 没有 `Any` 的序列化器，`List<Map<String, Any>>`
 * 会在运行时报 "Serializer for class 'Any' is not found"。
 * [JsonObject] 本身是可序列化的，取字段再用 [int] / [str] / [bool] 等帮助函数。
 */
@Serializable
data class RawTableData(
    val code: Int = 0,
    val msg: String = "",
    val rows: List<JsonObject> = emptyList(),
    val total: Long = 0
)
