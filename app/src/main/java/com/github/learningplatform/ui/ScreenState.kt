package com.github.learningplatform.ui

import com.github.learningplatform.core.network.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * ViewModel 统一异步执行模板。
 *
 * 项目里每个 ViewModel 都要写一遍 `try/catch(CancellationException) throw/catch(Throwable)`，
 * 抽到这里集中处理，避免各页面对协程取消、错误文案的处理不一致。
 *
 * @param onError 自定义失败分支（例如「已有更多数据时保留旧列表」），默认只回填 error 字段。
 */
suspend fun <S> MutableStateFlow<S>.apiCall(
    onStart: (S) -> S = { it },
    onError: (S, Throwable) -> S = { state, e -> state },
    block: suspend () -> S
) {
    value = onStart(value)
    try {
        value = block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        value = onError(value, e)
    }
}

/** 接口异常直接展示后端 message；网络/解析异常给统一文案 */
fun Throwable.readableMessage(): String =
    if (this is ApiException) message else "网络异常，请稍后重试"