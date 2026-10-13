package com.github.learningplatform.core.network

import kotlinx.serialization.json.Json

/**
 * 全局 JSON 配置。
 *
 * 抽出来是因为**网络解析和本地缓存必须用同一套规则**：
 * 缓存是把响应体序列化成字符串存进 Room，读回时再反序列化。
 * 若两边配置不一致（比如一边 `ignoreUnknownKeys = true` 另一边没开），
 * 会出现「接口能解析但缓存读不出」这类很难定位的问题。
 *
 * 改这里等同于同时改网络层和缓存层，别在别处再 new 一个 Json。
 */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}
