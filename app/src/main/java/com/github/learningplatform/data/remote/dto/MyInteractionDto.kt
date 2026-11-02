package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 我的点赞/收藏列表项
 */
@Serializable
data class MyInteractionDto(
    val interactionId: Long = 0,
    val targetType: String = "",
    val targetId: Long = 0,
    val title: String = "",
    val coverUrl: String = "",
    val author: String = "",
    val createTime: String = ""
)
