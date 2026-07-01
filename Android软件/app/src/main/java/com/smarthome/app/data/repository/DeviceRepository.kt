package com.smarthome.app.data.repository

import com.smarthome.app.core.model.DeviceStatus
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.remote.api.DeviceApi
import com.smarthome.app.data.remote.dto.DeviceControlRequest

class DeviceRepository constructor(
    private val deviceApi: DeviceApi
) {
    suspend fun getAllStatus(): NetworkResult<List<DeviceStatus>> {
        return try {
            val response = deviceApi.getAllStatus()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取设备状态失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取设备状态失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun control(deviceCode: String, action: Int): NetworkResult<Unit> {
        return try {
            val response = deviceApi.control(DeviceControlRequest(deviceCode, action))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "控制设备失败")
                }
            } else {
                NetworkResult.Error(response.code(), "控制设备失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun allOn(): NetworkResult<Unit> {
        return try {
            val response = deviceApi.allOn()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "一键全开失败")
                }
            } else {
                NetworkResult.Error(response.code(), "一键全开失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun allOff(): NetworkResult<Unit> {
        return try {
            val response = deviceApi.allOff()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "一键全关失败")
                }
            } else {
                NetworkResult.Error(response.code(), "一键全关失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
