package com.smarthome.app.data.repository

import com.smarthome.app.core.model.AiChatMessage
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.remote.api.AiApi
import com.smarthome.app.data.remote.dto.AiChatRequest

class AiRepository constructor(
    private val aiApi: AiApi
) {
    suspend fun chat(question: String, sessionId: String? = null): NetworkResult<AiChatMessage> {
        return try {
            val response = aiApi.chat(AiChatRequest(question, sessionId))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "AI对话失败")
                }
            } else {
                NetworkResult.Error(response.code(), "AI对话失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
