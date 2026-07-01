package com.smarthome.app.data.remote.api

import com.smarthome.app.core.model.WeatherData
import com.smarthome.app.core.network.ApiResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("/api/weather/current")
    suspend fun getCurrent(@Query("city") city: String? = null): Response<ApiResponseDto<WeatherData>>
}
