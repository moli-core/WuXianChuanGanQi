package com.smarthome.app.core.util

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val code: Int, val message: String) : NetworkResult<Nothing>()
    data class Exception(val e: Throwable) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error || this is Exception

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    fun errorMessage(): String = when (this) {
        is Error -> message
        is Exception -> e.message ?: "未知错误"
        else -> ""
    }
}
