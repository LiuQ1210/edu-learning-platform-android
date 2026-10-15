package com.github.learningplatform.data.remote

import com.github.learningplatform.core.network.ApiResponse
import com.github.learningplatform.data.remote.dto.AjaxResultDto
import com.github.learningplatform.data.remote.dto.ChangePasswordRequest
import com.github.learningplatform.data.remote.dto.SendCodeRequest
import com.github.learningplatform.data.remote.dto.SendCodeResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * 认证模块（匹配后端实际接口）。
 *
 * 后端为 Shiro Session 认证（非 JWT）：
 *  - GET  /captcha/captchaImage?type=char  → JPEG 图片流，验证码存 HttpSession
 *  - POST /api/login    表单 phone/password → AjaxResult（成功后种 JSESSIONID Cookie）
 *  - POST /api/register 表单 phone/password/confirmPassword → AjaxResult
 * 登录接口不校验图形验证码（后端未开启），验证码图片仅展示。
 */
interface AuthApi {

    /** 图形验证码：直接返回 JPEG 图片字节流 */
    @GET("captcha/captchaImage")
    suspend fun getCaptchaImage(@Query("type") type: String = "char"): Response<ResponseBody>

    /** 手机号密码登录（表单参数，非 JSON body） */
    @FormUrlEncoded
    @POST("api/login")
    suspend fun login(
        @Field("phone") phone: String,
        @Field("password") password: String
    ): Response<AjaxResultDto>

    /** 手机号注册 */
    @FormUrlEncoded
    @POST("api/register")
    suspend fun register(
        @Field("phone") phone: String,
        @Field("password") password: String,
        @Field("confirmPassword") confirmPassword: String
    ): Response<AjaxResultDto>

    // ---- 旧文档接口（后端未提供，保留兼容，页面无入口）----

    @POST("auth/send-code")
    suspend fun sendCode(@Body body: SendCodeRequest): Response<ApiResponse<SendCodeResponse>>

    @PUT("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<ApiResponse<Unit>>
}
