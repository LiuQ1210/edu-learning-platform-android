package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.ChangePasswordRequest
import com.github.learningplatform.data.remote.dto.LoginRequest
import com.github.learningplatform.data.remote.dto.LogoutRequest
import com.github.learningplatform.data.remote.dto.RefreshTokenRequest
import com.github.learningplatform.data.remote.dto.RefreshTokenResponse
import com.github.learningplatform.data.remote.dto.RegisterRequest
import com.github.learningplatform.data.remote.dto.ResetPasswordRequest
import com.github.learningplatform.data.remote.dto.SendCodeRequest
import com.github.learningplatform.data.remote.dto.SendCodeResponse
import com.github.learningplatform.data.remote.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * 认证模块（接口文档 v3.0 第二章）。
 *
 * 不提供第三方（微信等）登录入口：账号体系只走 send-code / register / login /
 * refresh / reset-password，微信登录接口与第三方账号绑定接口已随需求移除。
 * 匿名白名单：send-code / register / login / refresh / reset-password
 */
interface AuthApi {

    @POST("auth/send-code")
    suspend fun sendCode(@Body body: SendCodeRequest): Response<ApiResponse<SendCodeResponse>>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<ApiResponse<TokenResponse>>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiResponse<TokenResponse>>

    /** refreshToken 轮转：旧 refreshToken 返回后立即作废 */
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshTokenRequest): Response<ApiResponse<RefreshTokenResponse>>

    @POST("auth/logout")
    suspend fun logout(@Body body: LogoutRequest): Response<ApiResponse<Unit>>

    @PUT("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<ApiResponse<Unit>>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<ApiResponse<Unit>>
}
