package com.smarthome.app.data.repository

import com.smarthome.app.core.model.DeviceRanking
import com.smarthome.app.core.model.DeviceStats
import com.smarthome.app.core.model.EventDistribution
import com.smarthome.app.core.model.EventTrends
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.remote.api.ReportApi

class ReportRepository constructor(
    private val reportApi: ReportApi
) {
    suspend fun getDeviceStats(): NetworkResult<DeviceStats> {
        return try {
            val response = reportApi.getDeviceStats()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取设备统计失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取设备统计失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getEventTrends(days: Int = 30): NetworkResult<EventTrends> {
        return try {
            val response = reportApi.getEventTrends(days)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取事件趋势失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取事件趋势失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getEventDistribution(): NetworkResult<EventDistribution> {
        return try {
            val response = reportApi.getEventDistribution()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取事件分布失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取事件分布失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getDeviceRanking(limit: Int = 10): NetworkResult<List<DeviceRanking>> {
        return try {
            val response = reportApi.getDeviceRanking(limit)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取设备排行失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取设备排行失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
