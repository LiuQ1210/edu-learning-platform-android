package com.github.learningplatform.data.remote.dto

/**
 * 单条积分明细（后端 `GET /api/v1/points/logs` 的 rows）。
 *
 * - [changeAmount] 正数加分、负数扣分；
 * - [balance] 是本次变动后的余额快照，直接展示，不要让前端自己累加；
 * - [sourceLabel] 由服务端按 PointsSource 填中文名，客户端不维护映射；
 * - [remark] 是后端给的说明，可能为空。
 */
data class PointsLogDto(
    val logId: Long = 0,
    val changeAmount: Int = 0,
    val balance: Int = 0,
    val type: String = "",
    val sourceLabel: String = "",
    val remark: String = "",
    val refId: Long = 0,
    val createTime: String = ""
)
