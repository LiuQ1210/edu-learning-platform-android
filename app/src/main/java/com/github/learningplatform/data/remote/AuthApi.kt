package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.AuthResultDto
import com.github.learningplatform.data.remote.dto.CaptchaImageDto
import com.github.learningplatform.data.remote.dto.ChangePasswordRequest
import com.github.learningplatform.data.remote.dto.LoginRequest
import com.github.learningplatform.data.remote.dto.LogoutRequest
import com.github.learningplatform.data.remote.dto.RefreshTokenRequest
import com.github.learningplatform.data.remote.dto.RegisterRequest
import com.github.learningplatform.data.remote.dto.ResetPasswordRequest
import com.github.learningplatform.data.remote.dto.SendCodeRequest
import com.github.learningplatform.data.remote.dto.SendCodeResponse
import com.github.learningplatform.data.remote.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * 认证模块。
 *
 * 【登录注册联调】后端为若依框架，认证接口实际为：
 *  - POST /api/user/login        （图形验证码：username/password/code/uuid）
 *  - POST /api/user/register     （username/password/nickname/code/uuid）
 *  - POST /api/user/refreshToken
 *  - POST /api/user/logout
 *  - GET  /captchaImage          （图形验证码，根路径无 /api 前缀）
 * 认证链路响应为若依格式 {code, msg, token}，不走全局 ApiResponse。
 *
 * send-code / change-password / reset-password 为旧文档接口，后端未提供，
 * 方法保留以兼容现有调用方，但页面无入口、实际不会调用。
 */
interface AuthApi {

    /** 若依图形验证码：uuid + base64 图片 */
    @GET("captchaImage")
    suspend fun getCaptchaImage(): Response<CaptchaImageDto>

    @POST("api/user/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResultDto>

    @POST("api/user/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResultDto>

    @POST("api/user/refreshToken")
    suspend fun refresh(@Body body: RefreshTokenRequest): Response<AuthResultDto>

    @POST("api/user/logout")
    suspend fun logout(@Body body: LogoutRequest): Response<AuthResultDto>

    // ---- 旧文档接口（后端未提供，保留兼容，页面无入口）----

    @POST("auth/send-code")
    suspend fun sendCode(@Body body: SendCodeRequest): Response<ApiResponse<SendCodeResponse>>

    @PUT("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<ApiResponse<Unit>>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<ApiResponse<Unit>>
}
