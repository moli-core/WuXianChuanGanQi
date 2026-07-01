package com.wireless.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 模型配置实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelConfig {
    private Long id;
    private String modelName;       // 模型名称
    private String provider;        // openai/tongyi/deepseek/qwen
    private String apiUrl;          // API 地址
    private String apiKey;          // API Key (加密)
    private String modelId;         // 模型 ID
    private BigDecimal temperature; // 温度参数
    private Integer maxTokens;      // 最大 Token
    private String systemPrompt;    // 系统提示词
    private Integer isDefault;      // 是否默认
    private Integer isEnabled;      // 是否启用
    private Integer sortOrder;      // 排序
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
