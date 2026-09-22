package com.github.learningplatform.ui

/**
 * 分页竞态防护。
 *
 * 所有列表 ViewModel 都是「先快照 → 发请求 → 回来后把结果拼到快照上」的写法。
 * 如果在请求飞行途中用户切了分类/排序（触发 reset），旧请求回来后会把**旧筛选条件**
 * 的数据追加进**新筛选条件**的列表，并且 hasMore 会按混合长度计算。
 *
 * 做法：reset 时把 `generation` 自增；响应回来时若当前 generation 与发起时不一致，
 * 说明筛选条件已经变了，直接丢弃这次响应。
 *
 * 每个分页 UiState 需要提供 `generation: Int` 字段，并在 load(reset = true) 时 +1。
 */
interface PagedUiState {
    val generation: Int
}

/** 发起请求时记录的 generation 是否仍然是最新的一轮。 */
fun PagedUiState.isStaleRequest(generation: Int): Boolean = this.generation != generation
