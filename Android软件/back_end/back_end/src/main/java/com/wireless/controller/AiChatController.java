package com.wireless.controller;

import com.wireless.model.dto.AiChatRequest;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 接口 (对话 + 组句 — 与硬件同学统一)
 */
@Tag(name = "AI 问答", description = "智能对话、组句成文")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiService aiService;

    @Operation(summary = "AI 智能问答 (环境评估/故障分析)")
    @PostMapping("/chat")
    public ApiResult<Map<String, String>> chat(@RequestBody AiChatRequest request) {
        String reply = aiService.chat(request.getQuestion(), request.getSessionId());
        return ApiResult.success(Map.of(
                "question", request.getQuestion(),
                "reply", reply
        ));
    }

    @Operation(summary = "组词成句 (兼容硬件同学接口)")
    @PostMapping("/compose")
    public ApiResult<Map<String, Object>> compose(@RequestBody Map<String, Object> request) {
        String input = aiService.composeSentence(request);
        return ApiResult.success(Map.of(
                "input", input,
                "result", input,
                "model", "deepseek-chat"
        ));
    }
}
