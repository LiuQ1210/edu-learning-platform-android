package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

/** 分页响应，对齐文档 1.2：`{ "total": 128, "list": [] }` */
@Serializable
data class PageData<T>(
    val total: Long = 0,
    val list: List<T> = emptyList()
)