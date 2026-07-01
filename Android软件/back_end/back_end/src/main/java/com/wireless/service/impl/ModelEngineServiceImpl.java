package com.wireless.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wireless.mapper.AiModelConfigMapper;
import com.wireless.model.dto.ModelConfigRequest;
import com.wireless.model.entity.AiModelConfig;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.ModelEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelEngineServiceImpl implements ModelEngineService {

    private final AiModelConfigMapper modelConfigMapper;
    private final OkHttpClient okHttpClient;

    @Override
    public List<AiModelConfig> listModels() {
        return modelConfigMapper.selectAll();
    }

    @Override
    @Transactional
    public ApiResult<?> addModel(ModelConfigRequest request) {
        AiModelConfig config = AiModelConfig.builder()
                .modelName(request.getModelName())
                .provider(request.getProvider() != null ? request.getProvider() : "openai")
                .apiUrl(request.getApiUrl())
                .apiKey(request.getApiKey())
                .modelId(request.getModelId())
                .temperature(request.getTemperature() != null ? BigDecimal.valueOf(request.getTemperature()) : new BigDecimal("0.70"))
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 2048)
                .systemPrompt(request.getSystemPrompt())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : 0)
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : 1)
                .build();

        if (config.getIsDefault() == 1) {
            modelConfigMapper.clearDefault();
        }
        modelConfigMapper.insert(config);
        return ApiResult.success(config);
    }

    @Override
    @Transactional
    public ApiResult<?> updateModel(Long id, ModelConfigRequest request) {
        AiModelConfig config = AiModelConfig.builder()
                .id(id)
                .modelName(request.getModelName())
                .provider(request.getProvider())
                .apiUrl(request.getApiUrl())
                .apiKey(request.getApiKey())
                .modelId(request.getModelId())
                .temperature(request.getTemperature() != null ? BigDecimal.valueOf(request.getTemperature()) : null)
                .maxTokens(request.getMaxTokens())
                .systemPrompt(request.getSystemPrompt())
                .isDefault(request.getIsDefault())
                .isEnabled(request.getIsEnabled())
                .build();
        modelConfigMapper.update(config);
        return ApiResult.success();
    }

    @Override
    @Transactional
    public ApiResult<?> deleteModel(Long id) {
        modelConfigMapper.deleteById(id);
        return ApiResult.success();
    }

    @Override
    @Transactional
    public ApiResult<?> setDefault(Long id) {
        modelConfigMapper.clearDefault();
        modelConfigMapper.setDefault(id);
        return ApiResult.success();
    }

    @Override
    public ApiResult<?> testModel(Long id, String testMessage) {
        AiModelConfig config;
        if (id != null) {
            config = modelConfigMapper.selectById(id);
        } else {
            config = modelConfigMapper.selectDefault();
        }
        if (config == null) {
            return ApiResult.error("未找到可用的模型配置");
        }

        try {
            String reply = callOpenAICompatibleApi(config, testMessage);
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("question", testMessage);
            data.put("reply", reply);
            data.put("model", config.getModelName());
            return ApiResult.success(data);
        } catch (Exception e) {
            log.error("模型测试失败", e);
            return ApiResult.error("模型调用失败: " + e.getMessage());
        }
    }

    @Override
    public AiModelConfig getDefaultModel() {
        return modelConfigMapper.selectDefault();
    }

    /** 调用 OpenAI 兼容 API */
    private String callOpenAICompatibleApi(AiModelConfig config, String userMessage) throws IOException {
        JSONObject body = new JSONObject();
        body.put("model", config.getModelId());
        body.put("messages", List.of(
                Map.of("role", "system", "content", config.getSystemPrompt() != null ? config.getSystemPrompt() : "You are a helpful assistant."),
                Map.of("role", "user", "content", userMessage)
        ));
        body.put("temperature", config.getTemperature());
        body.put("max_tokens", config.getMaxTokens());

        Request request = new Request.Builder()
                .url(config.getApiUrl())
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toJSONString(), MediaType.parse("application/json")))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API error: " + response.code() + " " + response.body().string());
            }
            JSONObject result = JSON.parseObject(response.body().string());
            return result.getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content");
        }
    }
}
