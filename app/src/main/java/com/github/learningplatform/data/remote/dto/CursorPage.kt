package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 游标分页响应（评论专用），对齐文档 1.3：
 * `{ "hasMore": true, "nextCursor": "88005", "list": [] }`
 */
@Serializable
data class CursorPage<T>(
    val hasMore: Boolean = false,
    val nextCursor: String = "",
    val list: List<T> = emptyList()
)