package com.wireless.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 语音转文字模块
 *
 * TODO: 对接以下 AI 语音识别接口之一:
 * - 通义千问 Paraformer
 * - 讯飞语音听写
 * - 百度语音识别
 *
 * 调用方式: POST 音频字节流 → AI API → 返回识别文本
 */
@Slf4j
@Component
public class SpeechToText {

    /**
     * 语音转文字 (占位实现)
     *
     * @param audioData 音频字节流 (WAV/PCM)
     * @return 识别文本
     */
    public String recognize(byte[] audioData) {
        log.info("语音识别: audio size={} bytes (待对接 AI API)", audioData.length);

        // TODO: 实际对接代码示例 (通义千问 Paraformer)
        // 1. 构造 multipart/form-data 请求
        // 2. POST https://dashscope.aliyuncs.com/api/v1/services/audio/asr/transcription
        // 3. 解析返回 JSON → extract "text" 字段

        return null; // 返回 null 表示未对接
    }
}
