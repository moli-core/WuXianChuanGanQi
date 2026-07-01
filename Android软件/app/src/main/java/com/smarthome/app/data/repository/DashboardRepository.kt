package com.smarthome.app.data.repository

import com.smarthome.app.core.model.DashboardVO
import com.smarthome.app.core.model.EnvChartVO
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.remote.api.DataApi

class DashboardRepository constructor(
    private val dataApi: DataApi
) {
    suspend fun getDashboard(): NetworkResult<DashboardVO> {
        return try {
            val response = dataApi.getDashboard()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取仪表盘数据失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取仪表盘数据失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getChart(hours: Int = 24): NetworkResult<EnvChartVO> {
        return try {
            val response = dataApi.getChart(hours)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取图表数据失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取图表数据失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
