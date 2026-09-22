package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.BindResultDto
import com.github.learningplatform.data.remote.dto.BindThirdPartyRequest
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
import com.github.learningplatform.data.remote.dto.WechatBindPhoneRequest
import com.github.learningplatform.data.remote.dto.WechatLoginRequest
import com.github.learningplatform.data.remote.dto.WechatLoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * 认证模块（接口文档 v3.0 第二章，11 个接口）。
 * 匿名白名单：send-code / register / login / refresh / wechat-login / reset-password
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

    @POST("auth/wechat-login")
    suspend fun wechatLogin(@Body body: WechatLoginRequest): Response<ApiResponse<WechatLoginResponse>>

    @POST("auth/wechat-bind-phone")
    suspend fun wechatBindPhone(@Body body: WechatBindPhoneRequest): Response<ApiResponse<TokenResponse>>

    @POST("auth/bind-third-party")
    suspend fun bindThirdParty(@Body body: BindThirdPartyRequest): Response<ApiResponse<BindResultDto>>

    @DELETE("auth/unbind-third-party/{platform}")
    suspend fun unbindThirdParty(@Path("platform") platform: String): Response<ApiResponse<Unit>>

    @PUT("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<ApiResponse<Unit>>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<ApiResponse<Unit>>
}