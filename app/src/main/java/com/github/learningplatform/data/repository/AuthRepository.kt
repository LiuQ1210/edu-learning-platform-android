package com.github.learningplatform.data.repository

import android.util.Base64
import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.core.network.AuthInterceptor
import com.github.learningplatform.core.network.okBody
import com.github.learningplatform.data.local.TaskCenterStore
import com.github.learningplatform.data.local.UserPreferences
import com.github.learningplatform.data.remote.AuthApi
import com.github.learningplatform.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证仓库（v1 Token 版）。
 *
 * 后端 v1 用 Bearer Token 认证：
 *  - POST /api/v1/login    表单 phone/password → AjaxResult（返回 token）
 *  - POST /api/v1/register 表单 phone/password/confirmPassword → AjaxResult
 *  - POST /api/v1/logout   吊销 token
 *  - GET  /captcha/captchaImage?type=char → JPEG 图片流
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val userPreferences: UserPreferences,
    private val authInterceptor: AuthInterceptor,
    private val taskCenterStore: TaskCenterStore
) {

    /** 图形验证码：下载 JPEG 字节流，转 base64 返回给 UI */
    suspend fun getCaptchaImage(): String {
        val resp = authApi.getCaptchaImage("char")
        if (!resp.isSuccessful) throw ApiException(resp.code(), "验证码加载失败: ${resp.code()}")
        val bytes = resp.body()?.bytes() ?: throw ApiException(-1, "验证码响应为空")
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * 手机号密码登录（v1，返回 token）。
     *
     * @param rememberMe true=持久化登录态到 DataStore，下次冷启动自动登录；
     *                  false=token 只放内存拦截器，本次会话有效，进程结束后需重新登录。
     */
    suspend fun login(phone: String, password: String, rememberMe: Boolean): User {
        val resp = authApi.login(phone, password)
        if (!resp.isSuccessful) throw ApiException(resp.code(), "HTTP错误: ${resp.code()}")
        val body = resp.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) throw ApiException(body.code, body.msg.ifBlank { "登录失败" })

        val token = body.token.orEmpty()
        val userId = body.userId?.toString().orEmpty()
        val userName = body.userName.orEmpty().ifBlank { phone }
        val phoneNumber = body.phonenumber.orEmpty().ifBlank { phone }

        // token 先放内存，保证本次会话的请求都带鉴权
        authInterceptor.setToken(token)
        // 仅「记住我」才把登录态写入 DataStore；不记住时不落盘，冷启动回到登录页。
        // 同时清掉上一次可能残留的持久化登录资料，避免冷启动读到旧账号。
        if (rememberMe) {
            userPreferences.saveLoginInfo(
                token = token,
                userId = userId,
                userName = userName,
                phone = phoneNumber,
                nickname = userName
            )
        } else {
            // 清掉上一次可能残留的登录态资料，但保留手机号用于下次回填历史账号
            userPreferences.clearLoginInfo()
            userPreferences.saveLastPhone(phoneNumber)
        }

        return User(id = body.userId ?: 0, username = userName, nickname = userName, phone = phoneNumber)
    }

    /** 手机号注册（v1）。注册成功后回填手机号到登录页，密码不落盘 */
    suspend fun register(phone: String, password: String, confirmPassword: String): User {
        val resp = authApi.register(phone, password, confirmPassword)
        if (!resp.isSuccessful) throw ApiException(resp.code(), "HTTP错误: ${resp.code()}")
        val body = resp.body() ?: throw ApiException(-1, "响应体为空")
        if (body.code != 0) throw ApiException(body.code, body.msg.ifBlank { "注册失败" })
        // 记住本次注册的手机号：注册成功回登录页后自动回填，无需重复输入；密码不保存
        userPreferences.saveLastPhone(phone)
        return User(id = 0, username = phone, nickname = phone, phone = phone)
    }

    /** 登出：调后端吊销 token，清空本地登录态；保留最近手机号用于下次回填 */
    suspend fun logout() {
        try {
            authApi.logout()
        } catch (_: Exception) {
            // 后端吊销失败也不影响本地退出
        }
        authInterceptor.clearToken()
        userPreferences.clearOnLogout()
        taskCenterStore.clear()
    }

    /**
     * 修改密码（后端 v1 新增）。
     *
     * 后端改密成功后会在同一事务里吊销该账号**全部 Token**（含当前设备），
     * 所以这里不论返回什么都要清掉本地登录态，让 UI 回登录页重新登录。
     */
    suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ) {
        try {
            authApi.changePassword(oldPassword, newPassword, confirmPassword).okBody()
        } finally {
            authInterceptor.clearToken()
            userPreferences.clear()
        }
    }
}
