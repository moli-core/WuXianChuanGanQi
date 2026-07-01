package com.smarthome.app.data.remote.api

import com.smarthome.app.core.model.DeviceStatus
import com.smarthome.app.core.network.ApiResponseDto
import com.smarthome.app.data.remote.dto.DeviceControlRequest
import retrofit2.Response
import retrofit2.http.*

interface DeviceApi {
    @GET("/api/device/status")
    suspend fun getAllStatus(): Response<ApiResponseDto<List<DeviceStatus>>>

    @GET("/api/device/status/{deviceCode}")
    suspend fun getStatus(@Path("deviceCode") deviceCode: String): Response<ApiResponseDto<DeviceStatus>>

    @POST("/api/device/control")
    suspend fun control(@Body request: DeviceControlRequest): Response<ApiResponseDto<Map<String, Any?>>>

    @POST("/api/device/all-on")
    suspend fun allOn(): Response<ApiResponseDto<Map<String, Any?>>>

    @POST("/api/device/all-off")
    suspend fun allOff(): Response<ApiResponseDto<Map<String, Any?>>>
}
