package com.smarthome.app.data.remote.api

import com.smarthome.app.core.model.AiChatMessage
import com.smarthome.app.core.network.ApiResponseDto
import com.smarthome.app.data.remote.dto.AiChatRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AiApi {
    @POST("/api/ai/chat")
    suspend fun chat(@Body request: AiChatRequest): Response<ApiResponseDto<AiChatMessage>>
}
