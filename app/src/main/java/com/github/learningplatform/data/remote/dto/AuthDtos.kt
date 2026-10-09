package com.github.learningplatform.data.remote.dto

import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// 认证模块 DTO（接口文档 v3.0 第二章，11 个接口）
// ---------------------------------------------------------------------------

@Serializable
data class SendCodeRequest(
    /** 手机号或邮箱 */
    val target: String,
    /** 1-手机号 2-邮箱 */
    val targetType: Int,
    /** register/login/reset_password/change_phone/change_email */
    val scene: String
)

@Serializable
data class SendCodeResponse(val expireSeconds: Int = 300)

/**
 * 2.2 注册（v3.1 增补：双通道）。
 *
 * v3.0 只支持「手机号 + 短信验证码」；增补后统一用 `account` + `accountType`
 * 承载账号标识，手机号走短信、邮箱走邮件。
 *
 * 兼容性：服务端仍需接受旧调用方的 `phone` 字段（有 `phone` 且无 `account` 时
 * 等价于 `account=phone, accountType=1`）。新代码一律用 [account]。
 *
 * @param accountType 1-手机号（默认）2-邮箱
 */
@Serializable
data class RegisterRequest(
    val account: String,
    val password: String,
    val code: String,
    val accountType: Int = ACCOUNT_TYPE_PHONE,
    val nickname: String? = null,
    val email: String? = null
) {
    companion object {
        const val ACCOUNT_TYPE_PHONE = 1
        const val ACCOUNT_TYPE_EMAIL = 2
    }
}

/** 2.3 登录（v3.1：username 允许传用户名 / 手机号 / 邮箱） */
@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class UserBriefDto(
    val userId: Long = 0,
    val username: String = "",
    val nickname: String = "",
    val avatar: String = "",
    val phone: String = ""
)

/** 登录 / 注册 的成功响应（双 Token） */
@Serializable
data class TokenResponse(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresIn: Long = 0,
    val user: UserBriefDto? = null
)

/** 2.4 刷新 Token（refreshToken 轮转） */
@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long = 0
)

/** 2.5 登出（需携带 refreshToken 以便作废） */
@Serializable
data class LogoutRequest(val refreshToken: String)

/** 2.10 修改密码 */
@Serializable
data class ChangePasswordRequest(val oldPassword: String, val newPassword: String)

/** 2.11 找回密码 */
@Serializable
data class ResetPasswordRequest(val phone: String, val code: String, val newPassword: String)

// ---------------------------------------------------------------------------
// 3.1 / 3.2 个人资料
// ---------------------------------------------------------------------------

@Serializable
data class UserProfileDto(
    val userId: Long,
    val username: String = "",
    val nickname: String = "",
    val avatar: String = "",
    val phone: String = "",
    val email: String = "",
    /** 性别：0未知 1男 2女 */
    val gender: Int = 0,
    val bio: String = "",
    /** 连续签到天数 */
    val continueSignDay: Int = 0,
    val lastSignDate: String = "",
    val createTime: String = ""
)

@Serializable
data class UpdateProfileRequest(
    val nickname: String? = null,
    val avatar: String? = null,
    val gender: Int? = null,
    val bio: String? = null
)