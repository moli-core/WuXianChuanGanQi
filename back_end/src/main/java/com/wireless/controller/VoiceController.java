package com.wireless.controller;

import com.wireless.model.vo.ApiResult;
import com.wireless.service.VoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 语音控制接口
 * 接收硬件 ESP32 和前端小程序的语音文件
 */
@Tag(name = "语音控制", description = "语音上传、识别、指令执行")
@RestController
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;

    // ===== 硬件同学兼容接口 =====

    @Operation(summary = "语音控制灯光 (兼容硬件同学 POST /api/voice/light)")
    @PostMapping("/api/voice/light")
    public ApiResult<?> voiceLight(@RequestParam("audio") MultipartFile audio,
                                    @RequestParam(defaultValue = "16000") int sampleRate,
                                    @RequestParam(defaultValue = "1") int channels,
                                    @RequestParam(defaultValue = "pcm") String encoding) {
        if (audio.isEmpty()) {
            return ApiResult.badRequest("音频文件为空");
        }
        return voiceService.processVoice(audio, "hardware");
    }

    // ===== 原有接口 =====

    @Operation(summary = "上传语音文件 (前端微信小程序)")
    @PostMapping("/api/voice/upload")
    public ApiResult<?> uploadVoice(@RequestParam("file") MultipartFile file,
                                    @RequestParam(defaultValue = "wechat") String source) {
        if (file.isEmpty()) {
            return ApiResult.badRequest("语音文件为空");
        }
        return voiceService.processVoice(file, source);
    }

    @Operation(summary = "接收硬件语音流 (ESP32 WiFi 上传)")
    @PostMapping("/api/voice/stream")
    public ApiResult<?> receiveStream(@RequestBody byte[] audioData,
                                      @RequestParam(defaultValue = "hardware") String source) {
        if (audioData == null || audioData.length == 0) {
            return ApiResult.badRequest("语音数据为空");
        }
        return voiceService.processVoiceBytes(audioData, source);
    }

    // ===== 健康检查 (兼容硬件同学 GET /health) =====

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public ApiResult<?> health() {
        return ApiResult.success("ok");
    }
}
