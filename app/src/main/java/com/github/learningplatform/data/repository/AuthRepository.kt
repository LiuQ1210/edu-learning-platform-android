package com.github.learningplatform.data.repository

import android.util.Base64
import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.data.demo.preview
import com.github.learningplatform.data.local.UserPreferences
import com.github.learningplatform.data.remote.AuthApi
import com.github.learningplatform.data.remote.dto.AjaxResultDto
import com.github.learningplatform.data.remote.dto.ChangePasswordRequest
import com.github.learningplatform.data.remote.dto.SendCodeRequest
import com.github.learningplatform.data.remote.dto.SendCodeResponse
import com.github.learningplatform.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证仓库（匹配后端 Shiro Session 认证）。
 *
 * 后端实际：
 *  - GET /captcha/captchaImage?type=char → JPEG 图片流
 *  - POST /api/login  表单 phone/password → AjaxResult（成功后种 JSESSIONID Cookie）
 *  - POST /api/register 表单 phone/password/confirmPassword → AjaxResult
 * 认证靠 OkHttp CookieJar 自动管理 JSESSIONID，本地不存 token。
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val userPreferences: UserPreferences
) {

    /**
     * 图形验证码：下载 JPEG 字节流，转 base64 返回给 UI 直接显示。
     * 验证码内容存在后端 HttpSession（靠 Cookie 关联），无需 uuid。
     */
    suspend fun getCaptchaImage(): String {
        val resp = authApi.getCaptchaImage("char")
        if (!resp.isSuccessful) throw ApiException(resp.code(), "验证码加载失败: ${resp.code()}")
        val bytes = resp.body()?.bytes() ?: throw ApiException(-1, "验证码响应为空")
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** 手机号密码登录（表单提交，无验证码参数） */
    suspend fun login(phone: String, password: String): User {
        preview {
            persist(userId = "1001", userName = "user_1001", nickname = "张三", phone = "138****0000")
            return User(id = 1001, username = "user_1001", nickname = "张三", phone = "138****0000")
        }
        val resp = authApi.login(phone, password)
        if (!resp.isSuccessful) throw ApiException(resp.code(), "HTTP错误: ${resp.code()}")
        val body = resp.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) throw ApiException(body.code, body.msg.ifBlank { "登录失败" })
        val userId = body.userId?.toString().orEmpty()
        val userName = body.userName.orEmpty().ifBlank { phone }
        val phoneNumber = body.phonenumber.orEmpty().ifBlank { phone }
        persist(userId = userId, userName = userName, nickname = userName, phone = phoneNumber)
        return User(id = body.userId ?: 0, username = userName, nickname = userName, phone = phoneNumber)
    }

    /** 手机号注册 */
    suspend fun register(phone: String, password: String, confirmPassword: String): User {
        val resp = authApi.register(phone, password, confirmPassword)
        if (!resp.isSuccessful) throw ApiException(resp.code(), "HTTP错误: ${resp.code()}")
        val body = resp.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) throw ApiException(body.code, body.msg.ifBlank { "注册失败" })
        // 注册成功不自动登录，返回成功即可
        return User(id = 0, username = phone, nickname = phone, phone = phone)
    }

    suspend fun sendCode(target: String, targetType: Int, scene: String): Int =
        safeApiCall { authApi.sendCode(SendCodeRequest(target, targetType, scene)) }.expireSeconds

    suspend fun changePassword(oldPassword: String, newPassword: String) {
        safeApiCall { authApi.changePassword(ChangePasswordRequest(oldPassword, newPassword)) }
    }

    /** 登出：清空本地登录态（后端 Shiro 无显式登出 API，清 Cookie 即可） */
    suspend fun logout() {
        userPreferences.clear()
    }

    private suspend fun persist(userId: String, userName: String, nickname: String, phone: String) {
        userPreferences.saveLoginInfo(
            userId = userId,
            userName = userName,
            nickname = nickname,
            phone = phone
        )
    }
}
