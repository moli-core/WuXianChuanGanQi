package com.smarthome.app.data.remote.dto

import com.squareup.moshi.Json

data class RegisterRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "email") val email: String? = null
)
