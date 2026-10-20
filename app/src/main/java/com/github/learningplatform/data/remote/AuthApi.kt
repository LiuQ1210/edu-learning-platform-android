package com.github.learningplatform.data.remote

import com.github.learningplatform.data.remote.dto.AjaxResultDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 认证模块（匹配后端 v1 接口，Token 认证）。
 *
 * 后端 v1 接口用 Bearer Token 认证：
 *  - POST /api/v1/login    表单 phone/password → AjaxResult（返回 token）
 *  - POST /api/v1/register 表单 phone/password/confirmPassword → AjaxResult
 *  - POST /api/v1/logout   吊销当前 token
 *  - GET  /captcha/captchaImage?type=char → JPEG 图片流（验证码接口不校验，仅展示）
 *
 * 客户端后续请求在 Header 里带 Authorization: Bearer <token>
 */
interface AuthApi {

    /** 图形验证码：直接返回 JPEG 图片字节流 */
    @GET("captcha/captchaImage")
    suspend fun getCaptchaImage(@Query("type") type: String = "char"): Response<ResponseBody>

    /** 手机号密码登录（v1，返回 token） */
    @FormUrlEncoded
    @POST("api/v1/login")
    suspend fun login(
        @Field("phone") phone: String,
        @Field("password") password: String
    ): Response<AjaxResultDto>

    /** 手机号注册（v1） */
    @FormUrlEncoded
    @POST("api/v1/register")
    suspend fun register(
        @Field("phone") phone: String,
        @Field("password") password: String,
        @Field("confirmPassword") confirmPassword: String
    ): Response<AjaxResultDto>

    /** 退出登录（v1，吊销 token） */
    @POST("api/v1/logout")
    suspend fun logout(): Response<AjaxResultDto>
}
