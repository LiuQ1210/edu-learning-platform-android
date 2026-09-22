package com.github.learningplatform.core.network

import com.github.learningplatform.core.constants.Constants
import com.github.learningplatform.core.di.ApplicationScope
import com.github.learningplatform.data.local.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自动注入 `Authorization: Bearer <accessToken>`。
 *
 * Token 缓存在内存中，由 [UserPreferences.accessToken] 驱动更新，
 * 避免每个请求都 runBlocking 读 DataStore。
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val userPreferences: UserPreferences,
    @ApplicationScope scope: CoroutineScope
) : Interceptor {

    @Volatile
    private var cachedToken: String = ""

    init {
        scope.launch {
            userPreferences.accessToken.collect { cachedToken = it }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = cachedToken.ifBlank {
            runBlocking { userPreferences.accessToken.first() }.also { cachedToken = it }
        }
        val request = chain.request().newBuilder().apply {
            if (token.isNotBlank()) {
                header(Constants.HEADER_AUTHORIZATION, Constants.BEARER_PREFIX + token)
            }
        }.build()
        return chain.proceed(request)
    }
}