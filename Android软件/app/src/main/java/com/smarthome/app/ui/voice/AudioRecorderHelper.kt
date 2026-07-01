package com.smarthome.app.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.*

/**
 * 录音辅助类 - PCM 16kHz 16bit mono
 */
class AudioRecorderHelper {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    private var onAudioData: ((ByteArray) -> Unit)? = null

    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)

    fun start(onData: (ByteArray) -> Unit) {
        if (isRecording) return
        onAudioData = onData
        isRecording = true

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 4
        )

        audioRecord?.startRecording()

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(bufferSize)
            while (isRecording && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (bytesRead > 0) {
                    val data = buffer.copyOf(bytesRead)
                    onAudioData?.invoke(data)
                }
            }
        }
    }

    fun stop() {
        isRecording = false
        recordingJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    fun isRecording(): Boolean = isRecording

    companion object {
        fun hasPermission(context: android.content.Context): Boolean {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        }
    }
}
