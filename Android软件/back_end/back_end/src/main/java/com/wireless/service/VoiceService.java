package com.wireless.service;

import com.wireless.model.vo.ApiResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 语音处理服务
 */
public interface VoiceService {

    /** 处理语音文件，返回识别 + 语义解析 + 设备执行结果 */
    ApiResult<?> processVoice(MultipartFile audioFile, String source);

    /** 处理语音字节流 (ESP32 上传) */
    ApiResult<?> processVoiceBytes(byte[] audioData, String source);
}
