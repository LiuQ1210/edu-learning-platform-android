package com.github.learningplatform.data.repository

import com.github.learningplatform.core.network.ApiException
import com.github.learningplatform.core.network.safeApiCall
import com.github.learningplatform.data.demo.preview
import com.github.learningplatform.core.network.safeApiCallForUnit
import com.github.learningplatform.data.local.UserPreferences
import com.github.learningplatform.data.remote.AuthApi
import com.github.learningplatform.data.remote.dto.AuthResultDataDto
import com.github.learningplatform.data.remote.dto.AuthResultDto
import com.github.learningplatform.data.remote.dto.CaptchaImageDto
import com.github.learningplatform.data.remote.dto.ChangePasswordRequest
import com.github.learningplatform.data.remote.dto.LoginRequest
import com.github.learningplatform.data.remote.dto.LogoutRequest
import com.github.learningplatform.data.remote.dto.RegisterRequest
import com.github.learningplatform.data.remote.dto.ResetPasswordRequest
import com.github.learningplatform.data.remote.dto.SendCodeRequest
import com.github.learningplatform.data.remote.dto.TokenResponse
import com.github.learningplatform.data.remote.dto.UserBriefDto
import com.github.learningplatform.domain.model.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val userPreferences: UserPreferences
) {

    /** 若依成功码：code == 200 */
    private fun AuthResultDto.requireOk(): String {
        if (code != 200) {
            throw ApiException(code, msg.ifBlank { "操作失败($code)" })
        }
        // 实测 token 在 data.token（顶层 token / data.accessToken 兜底），且自带 "Bearer " 前缀
        val raw = token.ifBlank { data?.token.orEmpty().ifBlank { data?.accessToken.orEmpty() } }
        if (raw.isBlank()) throw ApiException(code, "登录响应缺少 token")
        // 去掉 "Bearer " 前缀后存储，AuthInterceptor 会再加回
        return raw.removePrefix("Bearer ").trim()
    }

    /** 后端 data 里的 refreshToken（同样去前缀），失败时用 accessToken 兜底 */
    private fun AuthResultDataDto?.refreshTokenOr(token: String): String =
        this?.refreshToken?.removePrefix("Bearer ")?.trim()?.ifBlank { token } ?: token

    /** Response 包装解析：非 2xx / 空 body 先抛，再按若依码判成功 */
    private fun retrofit2.Response<AuthResultDto>.requireOk(): String {
        if (!isSuccessful) throw ApiException(code(), "HTTP错误: ${code()}")
        val body = body() ?: throw ApiException(code(), "响应体为空")
        return body.requireOk()
    }

    /** 若依图形验证码：uuid + base64 图片 */
    suspend fun getCaptchaImage(): CaptchaImageDto {
        val body = authApi.getCaptchaImage()
        if (!body.isSuccessful) {
            throw ApiException(body.code(), "HTTP错误: ${body.code()}")
        }
        val dto = body.body() ?: throw ApiException(-1, "响应体为空")
        if (dto.code != 200) throw ApiException(dto.code, dto.msg.ifBlank { "验证码获取失败" })
        return dto
    }

    /** 密码登录（若依：username/password + 图形验证码 code/uuid） */
    suspend fun login(username: String, password: String, code: String, uuid: String): User {
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
        val resp = authApi.login(LoginRequest(username, password, code, uuid))
        val token = resp.requireOk()
        val body = resp.body() ?: AuthResultDto()
        // 后端登录响应带用户信息（userId/username/nickname/avatar），一并持久化
        persist(
            TokenResponse(
                accessToken = token,
                refreshToken = body.data.refreshTokenOr(token),
                user = UserBriefDto(
                    userId = body.data?.userId ?: 0L,
                    username = body.data?.username.orEmpty().ifBlank { username },
                    nickname = body.data?.nickname.orEmpty(),
                    avatar = body.data?.avatar.orEmpty(),
                    phone = ""
                )
            )
        )
        return User(
            id = body.data?.userId ?: 0L,
            username = body.data?.username.orEmpty().ifBlank { username },
            nickname = body.data?.nickname.orEmpty(),
            phone = ""
        )
    }

    /**
     * 注册（若依：username/password/nickname + 图形验证码 code/uuid）。
     * 注册成功即签发 token。
     */
    suspend fun register(
        username: String,
        password: String,
        nickname: String?,
        code: String,
        uuid: String
    ): User {
        val resp = authApi.register(RegisterRequest(username, password, nickname, code, uuid))
        val token = resp.requireOk()
        val body = resp.body() ?: AuthResultDto()
        persist(
            TokenResponse(
                accessToken = token,
                refreshToken = body.data.refreshTokenOr(token),
                user = UserBriefDto(
                    userId = body.data?.userId ?: 0L,
                    username = body.data?.username.orEmpty().ifBlank { username },
                    nickname = body.data?.nickname.orEmpty().ifBlank { nickname.orEmpty() },
                    avatar = body.data?.avatar.orEmpty(),
                    phone = ""
                )
            )
        )
        return User(
            id = body.data?.userId ?: 0L,
            username = body.data?.username.orEmpty().ifBlank { username },
            nickname = body.data?.nickname.orEmpty().ifBlank { nickname.orEmpty() },
            phone = ""
        )
    }

    /** @param scene register/login/reset_password/change_phone/change_email */
    suspend fun sendCode(target: String, targetType: Int, scene: String): Int =
        safeApiCall { authApi.sendCode(SendCodeRequest(target, targetType, scene)) }.expireSeconds

    suspend fun changePassword(oldPassword: String, newPassword: String) =
        safeApiCallForUnit { authApi.changePassword(ChangePasswordRequest(oldPassword, newPassword)) }

    suspend fun resetPassword(phone: String, code: String, newPassword: String) =
        safeApiCallForUnit { authApi.resetPassword(ResetPasswordRequest(phone, code, newPassword)) }

    /** 登出：清空本地登录态（接口失败也要清） */
    suspend fun logout() {
        val refreshToken = userPreferences.currentRefreshToken()
        if (refreshToken.isNotBlank()) {
            runCatching {
                val r = authApi.logout(LogoutRequest(refreshToken))
                // 若依登出：code==200 即成功；失败忽略，本地登录态照清
                r.isSuccessful && r.body()?.code == 200
            }
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
}

/** UI 预览模式使用的假 Token（仅编译期开关打开时写入 DataStore） */
private const val PREVIEW_TOKEN = "ui-preview-token"
