package com.github.learningplatform.core.network

import com.github.learningplatform.core.constants.Constants
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import retrofit2.Response

/**
 * 后端响应兼容层。
 *
 * 后端（RuoYi）有两套响应体，客户端必须同时吃下：
 *
 *  1) 表格 `TableDataInfo`：`{ code, msg, rows, total }`
 *  2) `AjaxResult`：
 *     - 部分接口把业务字段**平铺在顶层**：`{ code, msg, userId, userName, ... }`
 *       （Controller 里用 `ajax.putAll(...)` / `ajax.put("count", ...)` 拼出来的）
 *     - 部分接口把业务对象放在 `data` 下：`{ code, msg, data: {...} }`
 *       （Controller 里用 `AjaxResult.success(obj)`）
 *
 * 所以这里不按固定 DTO 解析，而是先拿 [JsonObject]，
 * 用 [payload] 统一取业务字段（有 `data` 就取 `data`，没有就用整个对象）。
 *
 * 成功码为 [Constants.CODE_SUCCESS]（后端已把 success 改成 0）。
 */
fun Response<JsonObject>.okBody(): JsonObject {
    if (!isSuccessful) {
        throw ApiException(code(), "HTTP错误: ${code()} ${message()}")
    }
    val body = body() ?: throw ApiException(-1, "响应体为空")
    val code = body.int("code")
    if (code != Constants.CODE_SUCCESS) {
        throw ApiException(code, body.str("msg"))
    }
    return body
}

/** 取业务字段容器：有 `data` 对象则取它，否则视为平铺，返回自身。 */
fun JsonObject.payload(): JsonObject {
    val data = this["data"]
    return if (data is JsonObject) data else this
}

/** 表格分页响应的成功校验：`{ code, msg, rows, total }`。 */
fun Response<RawTableData>.okTable(): RawTableData {
    if (!isSuccessful) {
        throw ApiException(code(), "HTTP错误: ${code()} ${message()}")
    }
    val body = body() ?: throw ApiException(-1, "响应体为空")
    if (body.code != Constants.CODE_SUCCESS) {
        throw ApiException(body.code, body.msg)
    }
    return body
}

// ---------------------------------------------------------------------------
// 取值帮助函数：字段缺失一律返回默认值，避免后端字段命名差异直接抛异常
// ---------------------------------------------------------------------------

fun JsonObject.int(key: String, default: Int = 0): Int =
    (this[key] as? JsonPrimitive)?.intOrNull ?: default

fun JsonObject.long(key: String, default: Long = 0L): Long =
    (this[key] as? JsonPrimitive)?.longOrNull ?: default

fun JsonObject.double(key: String, default: Double = 0.0): Double =
    (this[key] as? JsonPrimitive)?.doubleOrNull ?: default

fun JsonObject.bool(key: String, default: Boolean = false): Boolean =
    (this[key] as? JsonPrimitive)?.booleanOrNull ?: default

fun JsonObject.str(key: String, default: String = ""): String {
    val v = this[key]
    if (v == null || v is JsonNull) return default
    return (v as? JsonPrimitive)?.let { if (it.isString) it.content else it.content } ?: default
}

/** 平铺/包裹两种情况都取一遍：先业务容器，再顶层。 */
fun JsonObject.lookup(key: String): JsonElement? =
    payload()[key] ?: this[key]

fun JsonObject.listOf(key: String): List<JsonElement> =
    (lookup(key) as? JsonArray) ?: emptyList()

fun JsonObject.longList(key: String): List<Long> =
    listOf(key).mapNotNull { (it as? JsonPrimitive)?.longOrNull }

/** 把 JSON 值转成 Kotlin 基础类型（String/Boolean/Long/Double/List/Map） */
fun JsonElement.toAnyOrNull(): Any? = when (this) {
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> booleanOrNull as Boolean
        longOrNull != null -> longOrNull as Long
        doubleOrNull != null -> doubleOrNull as Double
        else -> content
    }
    is JsonObject -> toAnyMap()
    is JsonArray -> map { it.toAnyOrNull() }
    JsonNull -> null
}

/** JsonObject -> Map<String, Any>，供仍以 Map 消费的 UI 复用 */
fun JsonObject.toAnyMap(): Map<String, Any> =
    entries.associate { (k, v) -> k to (v.toAnyOrNull() ?: "") }
