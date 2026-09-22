package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.data.demo.preview
import com.github.learningplatform.core.network.safeApiCallForUnit
import com.github.learningplatform.data.local.UserPreferences
import com.github.learningplatform.data.remote.AuthApi
import com.github.learningplatform.data.remote.dto.ChangePasswordRequest
import com.github.learningplatform.data.remote.dto.LoginRequest
import com.github.learningplatform.data.remote.dto.LogoutRequest
import com.github.learningplatform.data.remote.dto.RegisterRequest
import com.github.learningplatform.data.remote.dto.ResetPasswordRequest
import com.github.learningplatform.data.remote.dto.SendCodeRequest
import com.github.learningplatform.data.remote.dto.TokenResponse
import com.github.learningplatform.data.remote.dto.UserBriefDto
import com.github.learningplatform.data.remote.dto.WechatBindPhoneRequest
import com.github.learningplatform.data.remote.dto.WechatLoginRequest
import com.github.learningplatform.data.remote.dto.WechatLoginResponse
import com.github.learningplatform.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val userPreferences: UserPreferences
) {

    /** 密码登录 */
    suspend fun login(username: String, password: String): User {
        // UI 预览模式：不发请求，直接落一个本地登录态，让四个 Tab 能进去看
        preview {
            persist(
                TokenResponse(
                    accessToken = PREVIEW_TOKEN,
                    refreshToken = PREVIEW_TOKEN,
                    expiresIn = 7200,
                    user = UserBriefDto(
                        userId = 1001,
                        username = "user_1001",
                        nickname = "张三",
                        avatar = "",
                        phone = "138****0000"
                    )
                )
            )
            return User(id = 1001, username = "user_1001", nickname = "张三", phone = "138****0000")
        }
        val data = safeApiCall { authApi.login(LoginRequest(username, password)) }
        persist(data)
        return data.toUser(username)
    }

    /**
     * 注册（v3.1 增补 2.2：手机号/邮箱双通道，注册成功即签发双 Token）。
     *
     * @param account 手机号或邮箱
     * @param accountType 1-手机号 2-邮箱（[RegisterRequest.ACCOUNT_TYPE_PHONE] / [RegisterRequest.ACCOUNT_TYPE_EMAIL]）
     * @param code 由 2.1 send-code 下发到对应通道的验证码
     */
    suspend fun register(
        account: String,
        password: String,
        code: String,
        accountType: Int = RegisterRequest.ACCOUNT_TYPE_PHONE,
        nickname: String? = null,
        email: String? = null
    ): User {
        val data = safeApiCall {
            authApi.register(
                RegisterRequest(
                    account = account,
                    password = password,
                    code = code,
                    accountType = accountType,
                    nickname = nickname,
                    // 邮箱注册时 email 与 account 相同，避免后端再要求一次
                    email = email ?: account.takeIf { accountType == RegisterRequest.ACCOUNT_TYPE_EMAIL }
                )
            )
        }
        persist(data)
        return data.toUser()
    }

    /** @param scene register/login/reset_password/change_phone/change_email */
    suspend fun sendCode(target: String, targetType: Int, scene: String): Int =
        safeApiCall { authApi.sendCode(SendCodeRequest(target, targetType, scene)) }.expireSeconds

    /** 微信登录：可能返回 needBindPhone，需再走 bindPhone 流程 */
    suspend fun wechatLogin(code: String): WechatLoginResponse {
        val data = safeApiCall { authApi.wechatLogin(WechatLoginRequest(code)) }
        if (!data.needBindPhone && data.accessToken.isNotBlank()) {
            persist(
                TokenResponse(
                    accessToken = data.accessToken,
                    refreshToken = data.refreshToken,
                    expiresIn = data.expiresIn,
                    user = data.user
                )
            )
        }
        return data
    }

    /** 微信新用户绑定手机号完成注册 */
    suspend fun wechatBindPhone(
        tempToken: String,
        phone: String,
        code: String,
        nickname: String? = null
    ): User {
        val data = safeApiCall {
            authApi.wechatBindPhone(WechatBindPhoneRequest(tempToken, phone, code, nickname))
        }
        persist(data)
        return data.toUser()
    }

    suspend fun changePassword(oldPassword: String, newPassword: String) =
        safeApiCallForUnit { authApi.changePassword(ChangePasswordRequest(oldPassword, newPassword)) }

    suspend fun resetPassword(phone: String, code: String, newPassword: String) =
        safeApiCallForUnit { authApi.resetPassword(ResetPasswordRequest(phone, code, newPassword)) }

    /** 登出：作废 refreshToken，并清空本地登录态（接口失败也要清） */
    suspend fun logout() {
        val refreshToken = userPreferences.currentRefreshToken()
        if (refreshToken.isNotBlank()) {
            runCatching { safeApiCallForUnit { authApi.logout(LogoutRequest(refreshToken)) } }
        }
        userPreferences.clear()
    }

    private suspend fun persist(data: TokenResponse) {
        userPreferences.saveLoginInfo(
            accessToken = data.accessToken,
            refreshToken = data.refreshToken,
            userId = data.user?.userId?.toString().orEmpty(),
            nickname = data.user?.nickname.orEmpty(),
            avatar = data.user?.avatar.orEmpty()
        )
    }

    private fun TokenResponse.toUser(fallbackUsername: String = ""): User = User(
        id = user?.userId ?: 0L,
        username = user?.username?.ifBlank { fallbackUsername } ?: fallbackUsername,
        nickname = user?.nickname.orEmpty(),
        avatar = user?.avatar.orEmpty(),
        phone = user?.phone.orEmpty()
    )
}

/** UI 预览模式使用的假 Token（仅编译期开关打开时写入 DataStore） */
private const val PREVIEW_TOKEN = "ui-preview-token"
