package com.smarthome.app.data.remote.api

import com.smarthome.app.core.model.User
import com.smarthome.app.core.network.ApiResponseDto
import com.smarthome.app.data.remote.dto.LoginRequest
import com.smarthome.app.data.remote.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponseDto<Map<String, Any?>>>

    @POST("/api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponseDto<String>>

    @GET("/api/auth/profile")
    suspend fun getProfile(): Response<ApiResponseDto<User>>

    @PUT("/api/auth/profile")
    suspend fun updateProfile(@Body body: Map<String, String>): Response<ApiResponseDto<User>>

    @PUT("/api/auth/password")
    suspend fun changePassword(@Body body: Map<String, String>): Response<ApiResponseDto<String>>
}
