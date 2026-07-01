package com.wireless.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * AI 对话消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Long id;
    private String sessionId;       // 会话 ID
    private Long userId;            // 用户 ID
    private Long modelId;           // 模型 ID
    private String role;            // user/assistant/system
    private String content;         // 消息内容
    private String actionResult;    // 设备执行结果 JSON
    private LocalDateTime createTime;
}
