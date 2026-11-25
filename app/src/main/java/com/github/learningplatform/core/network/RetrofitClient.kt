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
    private val json = AppJson

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(idempotencyInterceptor)
        .authenticator(tokenAuthenticator)
        .apply {
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

    /**
     * 智能体接口单独一份：问答要走大模型，30 秒的全局读超时不够用，
     * 这里把读超时放宽到 120 秒（异步任务仍然走「提交 + 轮询」，不受影响）。
     */
    @PublishedApi
    internal val agentRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(
            okHttpClient.newBuilder()
                .readTimeout(120, TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    inline fun <reified T> create(): T = retrofit.create(T::class.java)

    inline fun <reified T> createAgent(): T = agentRetrofit.create(T::class.java)
}
