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
 * 2.2 注册（后端为若依框架：图形验证码，无短信/邮箱通道）。
 *
 * 【登录注册联调】按若依 RegisterDTO 适配：
 *   username + password + nickname + code + uuid（uuid 来自 GET /captchaImage）
 */
@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String? = null,
    val code: String,
    val uuid: String
)

/** 2.3 登录（若依 LoginDTO：username/password + 图形验证码 code + uuid） */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val code: String,
    val uuid: String
)

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

// ---------------------------------------------------------------------------
// 【登录注册联调】若依框架认证扩展 DTO
// ---------------------------------------------------------------------------

/**
 * 若依统一响应（认证链路专用，不经过全局 ApiResponse）。
 *
 * 实测响应（2026-10-09 联调）：
 *   {"code":200,"msg":"操作成功","data":{
 *     "token":"Bearer eyJ...","refreshToken":"Bearer eyJ...",
 *     "userId":9,"username":"test002","nickname":"测试用户002","avatar":null}}
 * 注意：成功码是 code==200（不是全局约定的 0）、字段是 msg（不是 message）、
 * Token 在 **data.token**（不是顶层 token、不是 data.accessToken），且自带 "Bearer " 前缀。
 */
@Serializable
data class AuthResultDto(
    val code: Int = 0,
    val msg: String = "",
    val token: String = "",
    val data: AuthResultDataDto? = null
)

@Serializable
data class AuthResultDataDto(
    val token: String = "",
    val refreshToken: String = "",
    val accessToken: String = "",
    val userId: Long = 0,
    val username: String = "",
    val nickname: String = "",
    val avatar: String? = null
)

/**
 * 若依图形验证码（GET /captchaImage）。
 *
 * 返回 { code:200, msg, uuid, img, captchaEnabled }：
 *  - uuid：验证码标识，登录/注册时原样带回
 *  - img：base64 图片（可能带 data:image/png;base64, 前缀）
 */
@Serializable
data class CaptchaImageDto(
    val code: Int = 0,
    val msg: String = "",
    val uuid: String = "",
    val img: String = "",
    val captchaEnabled: Boolean = true
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