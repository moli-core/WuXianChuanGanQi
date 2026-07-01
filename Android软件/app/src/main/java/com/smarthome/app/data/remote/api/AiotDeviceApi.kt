package com.smarthome.app.data.remote.api

import com.smarthome.app.core.model.AiotDevice
import com.smarthome.app.core.network.ApiResponseDto
import com.smarthome.app.data.remote.dto.AiotDeviceRequest
import retrofit2.Response
import retrofit2.http.*

interface AiotDeviceApi {
    @GET("/api/aiot-device")
    suspend fun getDevices(
        @Query("keyword") keyword: String? = null,
        @Query("deviceType") deviceType: String? = null,
        @Query("onlineStatus") onlineStatus: Boolean? = null
    ): Response<ApiResponseDto<List<AiotDevice>>>

    @GET("/api/aiot-device/{deviceCode}")
    suspend fun getDevice(@Path("deviceCode") deviceCode: String): Response<ApiResponseDto<AiotDevice>>

    @POST("/api/aiot-device")
    suspend fun createDevice(@Body request: AiotDeviceRequest): Response<ApiResponseDto<AiotDevice>>

    @PUT("/api/aiot-device/{id}")
    suspend fun updateDevice(
        @Path("id") id: Long,
        @Body request: AiotDeviceRequest
    ): Response<ApiResponseDto<Map<String, Any?>>>

    @DELETE("/api/aiot-device/{id}")
    suspend fun deleteDevice(@Path("id") id: Long): Response<ApiResponseDto<Map<String, Any?>>>
}
