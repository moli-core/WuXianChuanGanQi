package com.smarthome.app.data.repository

import com.smarthome.app.core.model.AiotDevice
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.remote.api.AiotDeviceApi
import com.smarthome.app.data.remote.dto.AiotDeviceRequest

class AiotDeviceRepository constructor(
    private val aiotDeviceApi: AiotDeviceApi
) {
    suspend fun getDevices(
        keyword: String? = null,
        deviceType: String? = null,
        onlineStatus: Boolean? = null
    ): NetworkResult<List<AiotDevice>> {
        return try {
            val response = aiotDeviceApi.getDevices(keyword, deviceType, onlineStatus)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取设备列表失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取设备列表失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getDevice(deviceCode: String): NetworkResult<AiotDevice> {
        return try {
            val response = aiotDeviceApi.getDevice(deviceCode)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取设备详情失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取设备详情失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun createDevice(request: AiotDeviceRequest): NetworkResult<AiotDevice> {
        return try {
            val response = aiotDeviceApi.createDevice(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "添加设备失败")
                }
            } else {
                NetworkResult.Error(response.code(), "添加设备失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun updateDevice(id: Long, request: AiotDeviceRequest): NetworkResult<Unit> {
        return try {
            val response = aiotDeviceApi.updateDevice(id, request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "更新设备失败")
                }
            } else {
                NetworkResult.Error(response.code(), "更新设备失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun deleteDevice(id: Long): NetworkResult<Unit> {
        return try {
            val response = aiotDeviceApi.deleteDevice(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "删除设备失败")
                }
            } else {
                NetworkResult.Error(response.code(), "删除设备失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
