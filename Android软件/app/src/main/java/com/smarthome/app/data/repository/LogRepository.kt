package com.smarthome.app.data.repository

import com.smarthome.app.core.model.AlertLogItem
import com.smarthome.app.core.model.OperationLogItem
import com.smarthome.app.core.model.VoiceLogItem
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.remote.api.LogApi

class LogRepository constructor(
    private val logApi: LogApi
) {
    suspend fun getVoiceLogs(): NetworkResult<List<VoiceLogItem>> {
        return try {
            val response = logApi.getVoiceLogs()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取语音日志失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取语音日志失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getOperationLogs(deviceCode: String? = null): NetworkResult<List<OperationLogItem>> {
        return try {
            val response = logApi.getOperationLogs(deviceCode)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取操作日志失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取操作日志失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun getAlertLogs(
        alertType: String? = null,
        startTime: String? = null,
        endTime: String? = null
    ): NetworkResult<List<AlertLogItem>> {
        return try {
            val response = logApi.getAlertLogs(alertType, startTime, endTime)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "获取告警日志失败")
                }
            } else {
                NetworkResult.Error(response.code(), "获取告警日志失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }

    suspend fun handleAlert(id: Long): NetworkResult<Unit> {
        return try {
            val response = logApi.handleAlert(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess) {
                    NetworkResult.Success(Unit)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "处理告警失败")
                }
            } else {
                NetworkResult.Error(response.code(), "处理告警失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
