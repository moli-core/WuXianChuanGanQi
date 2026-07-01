package com.wireless.service.impl;

import com.wireless.mapper.AiotDeviceMapper;
import com.wireless.mapper.VoiceLogMapper;
import com.wireless.model.entity.VoiceLog;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.HuaweiIotService;
import com.wireless.service.VoiceService;
import com.wireless.service.XfyunAstService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 语音处理服务 — 完整链路
 * 语音 → 讯飞AST转文字 → 快控优先 / DeepSeek兜底 → 华为IoT下发
 */
@Slf4j
@Service
public class VoiceServiceImpl implements VoiceService {

    private final HuaweiIotService huaweiIotService;
    private final XfyunAstService xfyunAstService;
    private final AiotDeviceMapper aiotDeviceMapper;
    private final VoiceLogMapper voiceLogMapper;

    public VoiceServiceImpl(HuaweiIotService huaweiIotService,
                            XfyunAstService xfyunAstService,
                            AiotDeviceMapper aiotDeviceMapper,
                            VoiceLogMapper voiceLogMapper) {
        this.huaweiIotService = huaweiIotService;
        this.xfyunAstService = xfyunAstService;
        this.aiotDeviceMapper = aiotDeviceMapper;
        this.voiceLogMapper = voiceLogMapper;
    }

    @Override
    @Transactional
    public ApiResult<?> processVoice(MultipartFile audioFile, String source) {
        try {
            return processVoiceBytes(audioFile.getBytes(), source);
        } catch (Exception e) {
            log.error("语音文件读取失败", e);
            return ApiResult.error("语音文件读取失败");
        }
    }

    @Override
    @Transactional
    public ApiResult<?> processVoiceBytes(byte[] audioData, String source) {
        // 1. 语音转文字 (讯飞 AST)
        String voiceText = speechToText(audioData);
        log.info("[语音→文字] {}", voiceText);

        if (voiceText == null || voiceText.isBlank()) {
            return ApiResult.error("语音识别失败，请重新说话");
        }

        // 2. 快控优先 + 华为 IoT 下发
        try {
            Map<String, Object> iotResult = huaweiIotService.sendVoiceCommand(voiceText);

            // 3. 记录日志
            VoiceLog vl = VoiceLog.builder()
                    .rawText(voiceText)
                    .deviceCommand(JSON_STR(iotResult.get("controlCmd")))
                    .isValid(hasProperties(iotResult) ? 1 : 0)
                    .parseMethod("local")
                    .source(source)
                    .build();
            voiceLogMapper.insert(vl);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("voiceText", voiceText);
            resp.put("controlCmd", iotResult.get("controlCmd"));
            resp.put("cloudSent", iotResult.get("cloudSent"));
            return ApiResult.success(resp);
        } catch (Exception e) {
            log.error("华为 IoT 下发失败", e);
            return ApiResult.error("设备控制失败: " + e.getMessage());
        }
    }

    /**
     * 语音转文字: 优先讯飞 AST，失败时降级为 UTF-8 文本 (调试用)
     */
    private String speechToText(byte[] audioData) {
        // 先试讯飞 AST (真实语音识别)
        try {
            String text = xfyunAstService.audioToText(audioData);
            if (text != null && !text.isBlank()) return text;
        } catch (Exception e) {
            log.warn("讯飞 AST 识别失败，降级为文本模式: {}", e.getMessage());
        }

        // 降级: 当作 UTF-8 文本直接使用 (调试/测试)
        try {
            String text = new String(audioData, StandardCharsets.UTF_8).trim();
            if (!text.isEmpty() && text.length() < 500) return text;
        } catch (Exception ignored) {}

        return null;
    }

    private boolean hasProperties(Map<String, Object> result) {
        Object cmd = result.get("controlCmd");
        if (cmd instanceof Map<?, ?> m) {
            Object props = m.get("properties");
            return props instanceof Map<?, ?> pm && !pm.isEmpty();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private String JSON_STR(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        return obj.toString();
    }
}
