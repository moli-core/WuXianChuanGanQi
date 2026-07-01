package com.smarthome.app.data.remote.api

import com.smarthome.app.core.model.DashboardVO
import com.smarthome.app.core.model.EnvChartVO
import com.smarthome.app.core.model.EnvData
import com.smarthome.app.core.network.ApiResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DataApi {
    @GET("/api/data/dashboard")
    suspend fun getDashboard(): Response<ApiResponseDto<DashboardVO>>

    @GET("/api/data/latest")
    suspend fun getLatest(): Response<ApiResponseDto<EnvData>>

    @GET("/api/data/chart")
    suspend fun getChart(@Query("hours") hours: Int = 24): Response<ApiResponseDto<EnvChartVO>>
}
