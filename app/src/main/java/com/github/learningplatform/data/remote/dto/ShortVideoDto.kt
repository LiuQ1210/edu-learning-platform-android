package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * 短视频 DTO（对应后端 learning_short_video 表）
 */
@Serializable
data class ShortVideoDto(
    val videoId: Long = 0,
    val videoTitle: String = "",
    val videoUrl: String = "",
    val coverUrl: String = "",
    val intro: String = "",
    val duration: Int = 0,
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val favoriteCount: Int = 0,
    val commentCount: Int = 0,
    val userName: String = "",
    val createTime: String = ""
)
