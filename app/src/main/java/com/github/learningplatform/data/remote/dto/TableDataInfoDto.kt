package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

/** 若依后端 TableDataInfo 格式：{ code, msg, rows, total } */
@Serializable
data class TableDataInfo<T>(
    val code: Int = 0,
    val msg: String = "",
    val rows: List<T> = emptyList(),
    val total: Long = 0
)
