package com.smarthome.app.core.network

import com.squareup.moshi.Json

data class ApiResponseDto<T>(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: T?
) {
    val isSuccess: Boolean get() = code == 200
}

data class ApiResponseMap(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String?,
    @Json(name = "data") val data: Map<String, Any>?
)
