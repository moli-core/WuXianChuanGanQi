package com.wireless.controller;

import com.wireless.model.dto.TtsRequest;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.TtsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "TTS 语音", description = "文字转语音合成")
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
public class TtsController {

    private final TtsService ttsService;

    @Operation(summary = "文字合成语音")
    @PostMapping("/synthesize")
    public ApiResult<Map<String, Object>> synthesize(@RequestBody TtsRequest request) {
        return ApiResult.success(ttsService.synthesize(request.getText(), request.getVoice()));
    }
}
