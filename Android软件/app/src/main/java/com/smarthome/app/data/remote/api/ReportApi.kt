package com.smarthome.app.data.remote.api

import com.smarthome.app.core.model.DeviceStats
import com.smarthome.app.core.model.DeviceRanking
import com.smarthome.app.core.model.EventDistribution
import com.smarthome.app.core.model.EventTrends
import com.smarthome.app.core.network.ApiResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ReportApi {
    @GET("/api/report/device-stats")
    suspend fun getDeviceStats(): Response<ApiResponseDto<DeviceStats>>

    @GET("/api/report/event-trends")
    suspend fun getEventTrends(@Query("days") days: Int = 30): Response<ApiResponseDto<EventTrends>>

    @GET("/api/report/event-distribution")
    suspend fun getEventDistribution(): Response<ApiResponseDto<EventDistribution>>

    @GET("/api/report/device-ranking")
    suspend fun getDeviceRanking(@Query("limit") limit: Int = 10): Response<ApiResponseDto<List<DeviceRanking>>>
}
