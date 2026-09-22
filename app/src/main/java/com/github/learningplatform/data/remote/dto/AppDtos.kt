package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

// ---------------------- 13/14. 广告与 APP 配置 ----------------------

/** 13.1 广告位配置（客户端按 slotCode 加载 Taku 广告） */
@Serializable
data class AdSlotDto(
    val slotCode: String,
    val slotName: String = "",
    val takuSlotId: String = "",
    val slotType: Int = 1
)

/** 14.1 APP 全局配置（不含敏感密钥） */
@Serializable
data class AppConfigDto(
    val appTitle: String = "",
    val communityAuditEnabled: Boolean = false,
    val videoFreePreviewSeconds: Int = 180,
    val adEnabled: Boolean = true
)