package com.wireless.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 语音指令日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceLog {
    private Long id;
    private String rawText;
    private String semanticResult;
    private String deviceCommand;
    private Integer isValid;      // 0-无效 1-有效
    private String parseMethod;   // local/model
    private String source;        // hardware/wechat/app
    private String operator;      // 操作人
    private String ttsResponse;   // TTS 回复文本
    private LocalDateTime createTime;
}
