package com.github.learningplatform.data.local

import kotlinx.serialization.Serializable

/** 兑换记录状态：虚拟权益即时到账，实物需要等待发货 */
@Serializable
enum class ExchangeStatus(val label: String) {
    PENDING("待发货"),
    SHIPPED("已发货"),
    COMPLETED("已完成");

    companion object {
        fun forCategory(categoryKey: String): ExchangeStatus =
            if (categoryKey == "video" || categoryKey == "course") COMPLETED else PENDING
    }
}

/** 一条兑换记录，随兑换成功写入本机 DataStore（后端暂无兑换接口） */
@Serializable
data class ExchangeRecord(
    val id: String,
    val prizeId: String,
    val title: String,
    val emoji: String,
    val points: Int,
    val createdAt: Long,
    val status: ExchangeStatus = ExchangeStatus.PENDING
)
