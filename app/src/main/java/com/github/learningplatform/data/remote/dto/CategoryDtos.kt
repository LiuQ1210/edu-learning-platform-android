package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

/** 4.1 分类树节点（多级递归） */
@Serializable
data class CategoryNodeDto(
    val categoryId: Long,
    val parentId: Long = 0,
    val categoryName: String = "",
    val sort: Int = 0,
    val status: Int = 1,
    val iconUrl: String = "",
    val children: List<CategoryNodeDto> = emptyList()
)

// 4.2 / 5.1 的列表项统一为 ContentItemDto，已按 v3.1 增补 1.2 节独立定义于
// data/remote/dto/ContentItemDto.kt（主键固定 id，且必须回传 type）。

/** 5.2 搜索热词 */
@Serializable
data class HotWordDto(val keyword: String = "", val count: Long = 0)

@Serializable
data class HotWordListDto(val hotWordList: List<HotWordDto> = emptyList())