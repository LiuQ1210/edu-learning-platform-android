package com.github.learningplatform.core.network

import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.data.local.UserPreferences
import com.github.learningplatform.data.remote.AuthApi
import com.github.learningplatform.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 双 Token 自动刷新（接口文档 2.4）。
 *
 * 收到 HTTP 401 时，用 refreshToken 换新双 Token 并重放请求；
 * 刷新失败则清空登录态，由 UI 引导重新登录。
 *
 * 并发安全：OkHttp 可能并发触发多个 401，用锁 +「token 是否已被换过」判断，
 * 避免同一 refreshToken 被并发消费（v3 是轮转制，旧 refreshToken 会立即作废）。
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val userPreferences: UserPreferences,
    private val authApiProvider: Provider<AuthApi>
) : Authenticator {

    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val failedToken = response.request
            .header(Constants.HEADER_AUTHORIZATION)
            ?.removePrefix(Constants.BEARER_PREFIX)
            ?.trim()
            .orEmpty()

        synchronized(refreshLock) {
            // 其它线程可能已刷新成功：token 已变则直接重放
            val current = runBlocking { userPreferences.accessToken.first() }
            if (current.isNotBlank() && current != failedToken) {
                return response.request.newBuilder()
                    .header(Constants.HEADER_AUTHORIZATION, Constants.BEARER_PREFIX + current)
                    .build()
            }

            val refreshToken = runBlocking { userPreferences.refreshToken.first() }
            if (refreshToken.isBlank()) return null

            return runBlocking {
                runCatching {
                    val body = authApiProvider.get()
                        .refresh(RefreshTokenRequest(refreshToken))
                        .body()
                    val data = body?.data
                    if (body?.code == Constants.CODE_SUCCESS && data != null) {
                        userPreferences.saveTokens(data.accessToken, data.refreshToken)
                        response.request.newBuilder()
                            .header(
                                Constants.HEADER_AUTHORIZATION,
                                Constants.BEARER_PREFIX + data.accessToken
                            )
                            .build()
                    } else {
                        null
                    }
                }.getOrElse {
                    // refreshToken 失效 / 网络异常 -> 登出
                    userPreferences.clear()
                    null
                }
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}