package com.smarthome.app.core.model

import com.squareup.moshi.Json

data class User(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "username") val username: String = "",
    @Json(name = "password") val password: String? = null,
    @Json(name = "nickname") val nickname: String = "",
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "avatar") val avatar: String? = null,
    @Json(name = "role") val role: String = "user",
    @Json(name = "status") val status: Int = 1,
    @Json(name = "lastLogin") val lastLogin: String? = null,
    @Json(name = "createTime") val createTime: String? = null
)

data class LoginResponse(
    val token: String,
    val userId: Int,
    val username: String,
    val nickname: String,
    val role: String
)
