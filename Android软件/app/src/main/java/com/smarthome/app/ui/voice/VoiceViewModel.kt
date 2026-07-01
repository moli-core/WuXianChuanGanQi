package com.smarthome.app.ui.voice

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iflytek.sparkchain.core.SparkChain
import com.iflytek.sparkchain.core.SparkChainConfig
import com.iflytek.sparkchain.core.asr.ASR
import com.iflytek.sparkchain.core.asr.AsrCallbacks
import com.smarthome.app.core.util.CommandParser
import com.smarthome.app.core.util.NetworkResult
import com.smarthome.app.data.repository.DeviceRepository
import com.smarthome.app.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VoiceUiState(
    val isListening: Boolean = false,
    val recognizedText: String = "",
    val resultMessage: String = "",
    val isProcessing: Boolean = false,
    val error: String? = null,
    val isInitialized: Boolean = false
)

class VoiceViewModel(
    private val deviceRepository: DeviceRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var audioRecorder: AudioRecorderHelper? = null
    private var mAsr: ASR? = null
    private var asrInitialized = false

    private val asrCallbacks = object : AsrCallbacks {
        override fun onResult(asrResult: ASR.ASRResult, o: Any?) {
            val text = asrResult.bestMatchText ?: ""
            val status = asrResult.status
            _uiState.update { it.copy(recognizedText = text) }
            if (status == 2) {
                processVoiceCommand(text)
            }
        }

        override fun onError(asrError: ASR.ASRError, o: Any?) {
            val msg = "识别错误 [${asrError.code}]: ${asrError.errMsg}"
            _uiState.update { it.copy(error = msg, isListening = false, isProcessing = false) }
            stopListening()
        }

        override fun onBeginOfSpeech() {}
        override fun onEndOfSpeech() {}
    }

    fun initSDK(context: Context, appId: String, apiKey: String, apiSecret: String) {
        if (asrInitialized) return
        try {
            val config = SparkChainConfig.builder()
                .appID(appId)
                .apiKey(apiKey)
                .apiSecret(apiSecret)
            val ret = SparkChain.getInst().init(context, config)
            if (ret == 0) {
                _uiState.update { it.copy(isInitialized = true) }
                asrInitialized = true
            } else {
                _uiState.update { it.copy(error = "SDK初始化失败: $ret") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "SDK初始化异常: ${e.localizedMessage}") }
        }
    }

    fun startListening(context: Context) {
        if (!AudioRecorderHelper.hasPermission(context)) {
            _uiState.update { it.copy(error = "请授予麦克风权限") }
            return
        }
        if (!asrInitialized) {
            _uiState.update { it.copy(error = "SDK未初始化") }
            return
        }
        _uiState.update { it.copy(isListening = true, recognizedText = "", resultMessage = "", error = null) }

        if (mAsr == null) {
            mAsr = ASR()
            mAsr?.registerCallbacks(asrCallbacks)
        }
        mAsr?.language("zh_cn")
        mAsr?.domain("iat")
        mAsr?.accent("mandarin")
        mAsr?.dwa("wpgs")          // 动态修正

        // 必须先 start 再 write！
        mAsr?.start(System.currentTimeMillis().toString())

        audioRecorder = AudioRecorderHelper()
        audioRecorder?.start { audioData ->
            mAsr?.write(audioData)
        }
    }

    fun stopListening() {
        mAsr?.stop(false)
        audioRecorder?.stop()
        _uiState.update { it.copy(isListening = false) }
    }

    private fun processVoiceCommand(text: String) {
        if (text.isBlank()) return
        _uiState.update { it.copy(isProcessing = true) }

        val deviceCmd = CommandParser.parseDeviceCommand(text)
        if (deviceCmd != null) {
            val actionText = if (deviceCmd.action == 1) "打开" else "关闭"
            viewModelScope.launch {
                when (val result = deviceRepository.control(deviceCmd.deviceCode, deviceCmd.action)) {
                    is NetworkResult.Success -> _uiState.update { it.copy(resultMessage = "$actionText${deviceCmd.displayName} 成功", isProcessing = false) }
                    is NetworkResult.Error -> _uiState.update { it.copy(resultMessage = "$actionText${deviceCmd.displayName} 失败: ${result.message}", isProcessing = false) }
                    is NetworkResult.Exception -> _uiState.update { it.copy(resultMessage = "$actionText${deviceCmd.displayName} 失败", isProcessing = false) }
                    else -> {}
                }
            }
            return
        }

        val city = CommandParser.parseWeatherQuery(text)
        if (city != null) {
            viewModelScope.launch {
                when (val result = weatherRepository.getCurrentWeather(city)) {
                    is NetworkResult.Success -> {
                        val w = result.data
                        _uiState.update { it.copy(resultMessage = "${w.city ?: city}: ${w.temperature ?: "--"}°C ${w.weather ?: "--"}", isProcessing = false) }
                    }
                    else -> _uiState.update { it.copy(resultMessage = "天气查询失败", isProcessing = false) }
                }
            }
            return
        }

        _uiState.update { it.copy(resultMessage = "未识别的指令: $text", isProcessing = false) }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder?.stop()
        mAsr = null
    }
}
