package com.github.learningplatform.domain.model

/**
 * 领域层用户模型（与后端 DTO 解耦）。
 * 字段对齐 user 表：id/username/nickname/avatar/phone/email/gender/bio
 */
data class User(
    val id: Long,
    val username: String,
    val nickname: String,
    val avatar: String = "",
    val phone: String = "",
    val email: String = "",
    /** 性别：0未知 1男 2女 */
    val gender: Int = 0,
    val bio: String = ""
)