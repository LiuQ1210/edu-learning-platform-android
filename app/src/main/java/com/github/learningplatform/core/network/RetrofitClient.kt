package com.github.learningplatform.core.network

import com.github.learningplatform.BuildConfig
import com.github.learningplatform.core.constants.Constants
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitClient @Inject constructor(
    authInterceptor: AuthInterceptor,
    idempotencyInterceptor: IdempotencyInterceptor,
    tokenAuthenticator: TokenAuthenticator
) {
    // 用共享的 AppJson，不要在这里另建一份：
    // 缓存层要把响应序列化成字符串存进 Room、读回时再反序列化，
    // 两处配置不一致会出现「接口能解析但缓存读不出」这类很难定位的问题。
    private val json = AppJson

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(idempotencyInterceptor)
        .authenticator(tokenAuthenticator)
        .apply {
            // 仅 debug 打印报文：BODY 级别会输出 Token 与密码明文
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
            }
        }
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    inline fun <reified T> create(): T = retrofit.create(T::class.java)
}