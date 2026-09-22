package com.github.learningplatform.core.network

import com.github.learningplatform.core.constants.Constants
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 幂等 Token 拦截器（接口文档 v3.0 全局约定）。
 *
 * 所有写操作（POST/PUT/PATCH/DELETE）需携带 `X-Idempotent-Token`。
 *
 * 实现说明：若调用方已显式设置该头则保留；否则按请求生成一个 UUID。
 * TokenAuthenticator 重放请求时复用原始 Request，因此重试会携带同一个 Token，
 * 这正是「防重复提交」需要的行为。
 */
@Singleton
class IdempotencyInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val needsToken = request.method != "GET" && request.method != "HEAD"
        if (!needsToken || request.header(Constants.HEADER_IDEMPOTENT) != null) {
            return chain.proceed(request)
        }
        val newRequest = request.newBuilder()
            .header(Constants.HEADER_IDEMPOTENT, UUID.randomUUID().toString())
            .build()
        return chain.proceed(newRequest)
    }
}