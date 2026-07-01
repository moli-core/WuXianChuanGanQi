package com.wireless.model.dto;

import lombok.Data;

/**
 * AI 问答请求
 */
@Data
public class AiChatRequest {
    /** 用户问题 */
    private String question;
    /** 会话 ID (可选) */
    private String sessionId;
}
