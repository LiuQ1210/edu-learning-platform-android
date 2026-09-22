package com.github.learningplatform.core.network

/**
 * 统一结果封装，UI层通过StateFlow观察
 */
sealed interface Result<out T> {
    data object Loading : Result<Nothing>
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable, val code: Int = -1) : Result<Nothing>
}
