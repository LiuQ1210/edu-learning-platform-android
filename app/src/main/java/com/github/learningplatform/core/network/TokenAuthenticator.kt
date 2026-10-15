package com.github.learningplatform.core.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 401 自动重认证（Shiro Session 版）。
 *
 * Shiro 用 JSESSIONID Cookie，Session 过期后端返回 401/未登录页面，
 * 没有 refreshToken 机制——此处不再自动刷新，保留类以兼容 DI 注入链。
 */
@Singleton
class TokenAuthenticator @Inject constructor() : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? = null
}
