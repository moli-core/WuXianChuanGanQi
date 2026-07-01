package com.smarthome.app.data.remote.api

import com.smarthome.app.core.model.AlertLogItem
import com.smarthome.app.core.model.OperationLogItem
import com.smarthome.app.core.model.VoiceLogItem
import com.smarthome.app.core.network.ApiResponseDto
import retrofit2.Response
import retrofit2.http.*

interface LogApi {
    @GET("/api/log/voice")
    suspend fun getVoiceLogs(): Response<ApiResponseDto<List<VoiceLogItem>>>

    @GET("/api/log/operation")
    suspend fun getOperationLogs(
        @Query("deviceCode") deviceCode: String? = null
    ): Response<ApiResponseDto<List<OperationLogItem>>>

    @GET("/api/log/alert")
    suspend fun getAlertLogs(
        @Query("alertType") alertType: String? = null,
        @Query("startTime") startTime: String? = null,
        @Query("endTime") endTime: String? = null
    ): Response<ApiResponseDto<List<AlertLogItem>>>

    @PUT("/api/log/alert/{id}/handle")
    suspend fun handleAlert(@Path("id") id: Long): Response<ApiResponseDto<Map<String, Any?>>>
}
