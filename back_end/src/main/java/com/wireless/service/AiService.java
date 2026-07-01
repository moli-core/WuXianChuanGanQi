package com.wireless.service;

/**
 * AI 服务 (对话 / 问答)
 */
import java.util.Map;

public interface AiService {

    /** AI 智能问答 */
    String chat(String question, String sessionId);

    /** 组词成句 (兼容硬件同学) */
    String composeSentence(Map<String, Object> request);
}
