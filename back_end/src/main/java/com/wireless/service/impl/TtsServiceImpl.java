package com.wireless.service.impl;

import com.wireless.service.TtsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TtsServiceImpl implements TtsService {

    @Override
    public Map<String, Object> synthesize(String text, String voice) {
        Map<String, Object> result = new HashMap<>();
        result.put("text", text);
        result.put("voice", voice != null ? voice : "default");

        // TODO: 对接 TTS API (讯飞 / 百度 / 通义千问 CosyVoice)
        // 1. POST text 到 TTS API
        // 2. 获取音频字节流
        // 3. 存为文件并缓存
        // 4. 返回文件 URL
        log.info("TTS 合成请求: text={}, voice={}", text, voice);
        result.put("audioUrl", "placeholder — 请对接 TTS API");
        return result;
    }
}
