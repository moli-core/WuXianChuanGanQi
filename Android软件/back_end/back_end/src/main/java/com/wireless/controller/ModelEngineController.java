package com.wireless.controller;

import com.wireless.model.dto.ModelConfigRequest;
import com.wireless.model.entity.AiModelConfig;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.ModelEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "模型引擎", description = "AI 大模型配置管理、测试")
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class ModelEngineController {

    private final ModelEngineService modelEngineService;

    @Operation(summary = "模型列表")
    @GetMapping("/config")
    public ApiResult<List<AiModelConfig>> list() {
        return ApiResult.success(modelEngineService.listModels());
    }

    @Operation(summary = "新增模型配置")
    @PostMapping("/config")
    public ApiResult<?> add(@RequestBody ModelConfigRequest request) {
        return modelEngineService.addModel(request);
    }

    @Operation(summary = "修改模型配置")
    @PutMapping("/config/{id}")
    public ApiResult<?> update(@PathVariable Long id, @RequestBody ModelConfigRequest request) {
        return modelEngineService.updateModel(id, request);
    }

    @Operation(summary = "删除模型配置")
    @DeleteMapping("/config/{id}")
    public ApiResult<?> delete(@PathVariable Long id) {
        return modelEngineService.deleteModel(id);
    }

    @Operation(summary = "设为默认模型")
    @PutMapping("/default/{id}")
    public ApiResult<?> setDefault(@PathVariable Long id) {
        return modelEngineService.setDefault(id);
    }

    @Operation(summary = "测试模型 (发送消息并获取回复)")
    @PostMapping("/test")
    public ApiResult<?> test(@RequestParam(required = false) Long id,
                             @RequestParam String message) {
        return modelEngineService.testModel(id, message);
    }
}
