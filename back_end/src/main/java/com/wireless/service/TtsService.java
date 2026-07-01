package com.wireless.service;

import java.util.Map;

public interface TtsService {
    /** 文字合成语音，返回音频 URL */
    Map<String, Object> synthesize(String text, String voice);
}
