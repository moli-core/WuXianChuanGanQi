package com.smarthome.app.data.repository

import android.content.Context
import android.net.Uri
import com.smarthome.app.core.model.VoiceResult
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.remote.api.VoiceApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

class VoiceRepository constructor(
    private val voiceApi: VoiceApi
) {
    suspend fun uploadAudio(
        context: Context,
        audioUri: Uri,
        source: String = "app"
    ): NetworkResult<VoiceResult> {
        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(audioUri)
                ?: return NetworkResult.Error(400, "无法读取音频文件")
            val bytes = inputStream.readBytes()
            inputStream.close()

            val requestFile = bytes.toRequestBody("audio/wav".toMediaTypeOrNull())
            val audioPart = MultipartBody.Part.createFormData("file", "recording.wav", requestFile)
            val sourcePart = source.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = voiceApi.upload(audioPart, sourcePart)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.isSuccess && body.data != null) {
                    NetworkResult.Success(body.data)
                } else {
                    NetworkResult.Error(body?.code ?: 500, body?.message ?: "语音上传失败")
                }
            } else {
                NetworkResult.Error(response.code(), "语音上传失败: ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Exception(e)
        }
    }
}
