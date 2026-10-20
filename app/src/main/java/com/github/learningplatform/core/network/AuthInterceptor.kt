package com.github.learningplatform.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证拦截器（v1 Token 版）。
 *
 * 从内存变量读 token，加到 Authorization: Bearer <token> header。
 * 登录成功后由 AuthRepository 调 setToken() 更新内存缓存。
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    @Volatile
    private var token: String = ""

    fun setToken(newToken: String) {
        token = newToken
    }

    fun clearToken() {
        token = ""
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        // 已经有 Authorization 头就不重复加
        if (token.isBlank() || original.header("Authorization") != null) {
            return chain.proceed(original)
        }
        val newRequest = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(newRequest)
    }
}
