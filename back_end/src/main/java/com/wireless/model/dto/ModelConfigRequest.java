package com.wireless.model.dto;

import lombok.Data;

/**
 * 模型配置请求
 */
@Data
public class ModelConfigRequest {
    private String modelName;
    private String provider;
    private String apiUrl;
    private String apiKey;
    private String modelId;
    private Double temperature;
    private Integer maxTokens;
    private String systemPrompt;
    private Integer isDefault;
    private Integer isEnabled;
}
