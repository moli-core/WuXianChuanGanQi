package com.wireless.model.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 语音请求
 */
@Data
public class VoiceRequest {
    /** 语音文件 */
    private MultipartFile audioFile;
    /** 语音来源 hardware/wechat */
    private String source;
}
