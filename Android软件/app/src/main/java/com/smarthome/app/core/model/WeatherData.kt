package com.smarthome.app.core.model

import com.squareup.moshi.Json

data class WeatherData(
    @Json(name = "city") val city: String? = null,
    @Json(name = "temperature") val temperature: String? = null,
    @Json(name = "weather") val weather: String? = null,
    @Json(name = "humidity") val humidity: String? = null,
    @Json(name = "windDirection") val windDirection: String? = null,
    @Json(name = "windSpeed") val windSpeed: String? = null,
    @Json(name = "updateTime") val updateTime: String? = null
)
