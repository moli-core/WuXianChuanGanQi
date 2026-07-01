package com.smarthome.app.data.remote.api

import com.smarthome.app.core.model.VoiceResult
import com.smarthome.app.core.network.ApiResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VoiceApi {
    @Multipart
    @POST("/api/voice/upload")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Part("source") source: RequestBody
    ): Response<ApiResponseDto<VoiceResult>>
}
