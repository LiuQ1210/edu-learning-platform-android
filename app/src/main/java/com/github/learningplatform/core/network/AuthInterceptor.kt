package com.github.learningplatform.core.network

import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.core.di.ApplicationScope
import com.github.learningplatform.data.local.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证拦截器（v1 Token 版）：给每个请求加 `Authorization: Bearer <token>`。
 *
 * 之前这里只维护内存字段，登录时 `setToken()` 赋值 —— 进程重启后（冷启动、
 * 重装、被系统回收）内存被清空，但 DataStore 里 token 还在，于是
 * UI 认为"已登录"、请求却不带 Token，后端一律返回 401「未登录或登录超时」。
 * 现在改为订阅 [UserPreferences.token]，启动即自动恢复，登录/登出也会同步。
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val userPreferences: UserPreferences,
    @ApplicationScope scope: CoroutineScope
) : Interceptor {

    @Volatile
    private var token: String = ""

    init {
        // DataStore 第一次读是异步的，这里订阅后自动跟上；
        // 订阅之前就发出的请求由 intercept() 里的 runBlocking 兜底读取。
        scope.launch {
            userPreferences.token.collect { token = it }
        }
    }

    fun setToken(newToken: String) {
        token = newToken
    }

    fun clearToken() {
        token = ""
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // 调用方已显式带 Authorization 就不覆盖
        if (original.header(Constants.HEADER_AUTHORIZATION) != null) {
            return chain.proceed(original)
        }

        // 内存为空时同步读一次 DataStore，避免冷启动首批请求漏带 Token
        val current = token.ifBlank {
            runBlocking { runCatching { userPreferences.getToken() }.getOrDefault("") }
                .also { token = it }
        }
        if (current.isBlank()) {
            return chain.proceed(original)
        }

        val newRequest = original.newBuilder()
            .header(Constants.HEADER_AUTHORIZATION, Constants.BEARER_PREFIX + current)
            .build()
        return chain.proceed(newRequest)
    }
}
