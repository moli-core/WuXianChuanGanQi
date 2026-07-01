package com.wireless.model.dto;

import lombok.Data;

/**
 * TTS 合成请求
 */
@Data
public class TtsRequest {
    private String text;
    private String voice;
}
