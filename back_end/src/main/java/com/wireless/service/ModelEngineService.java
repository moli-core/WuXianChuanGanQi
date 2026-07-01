package com.wireless.service;

import com.wireless.model.entity.AiModelConfig;
import com.wireless.model.vo.ApiResult;
import com.wireless.model.dto.ModelConfigRequest;

import java.util.List;

public interface ModelEngineService {
    List<AiModelConfig> listModels();
    ApiResult<?> addModel(ModelConfigRequest request);
    ApiResult<?> updateModel(Long id, ModelConfigRequest request);
    ApiResult<?> deleteModel(Long id);
    ApiResult<?> setDefault(Long id);
    ApiResult<?> testModel(Long id, String testMessage);

    /** 获取当前默认模型配置 */
    AiModelConfig getDefaultModel();
}
