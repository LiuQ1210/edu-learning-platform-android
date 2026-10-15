package com.github.learningplatform.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证拦截器（Shiro Session 版）。
 *
 * 后端用 JSESSIONID Cookie 认证，OkHttp CookieJar 自动管理 Cookie，
 * 此拦截器不再注入 Authorization header，保留类以兼容 DI 注入链。
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
