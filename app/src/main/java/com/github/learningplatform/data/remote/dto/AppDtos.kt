package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

// ---------------------- 13. 广告 ----------------------

/** 13.1 广告位配置（客户端按 slotCode 加载 Taku 广告） */
@Serializable
data class AdSlotDto(
    val slotCode: String,
    val slotName: String = "",
    val takuSlotId: String = "",
    val slotType: Int = 1
)