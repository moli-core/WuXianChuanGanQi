package com.smarthome.app.data.repository

import com.smarthome.app.core.model.WeatherData
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.remote.api.WeatherApi
import java.net.URLEncoder

class WeatherRepository constructor(
    private val weatherApi: WeatherApi
) {
    suspend fun getCurrentWeather(city: String? = null): NetworkResult<WeatherData> {
        return try {
            val encodedCity = city?.takeIf { it.isNotBlank() }?.let {
                URLEncoder.encode(it, "UTF-8")
            }
            val response = weatherApi.getCurrent(encodedCity)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取天气失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取天气失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
