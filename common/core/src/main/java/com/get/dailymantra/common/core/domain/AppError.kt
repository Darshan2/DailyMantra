package com.get.dailymantra.common.core.domain

sealed class AppError(val isRetryable: Boolean) {
    data class Network(val cause: Throwable) : AppError(isRetryable = true)
    data class Server(val code: Int, val message: String?) : AppError(isRetryable = true)
    data class Auth(val message: String?) : AppError(isRetryable = false)
    data class NotFound(val message: String?) : AppError(isRetryable = false)
    data class Unknown(val message: String?, val cause: Throwable? = null) : AppError(isRetryable = false)
}
