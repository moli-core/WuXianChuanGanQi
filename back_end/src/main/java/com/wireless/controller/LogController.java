package com.wireless.controller;

import com.wireless.mapper.AlertLogMapper;
import com.wireless.mapper.OperationLogMapper;
import com.wireless.mapper.VoiceLogMapper;
import com.wireless.model.entity.AlertLog;
import com.wireless.model.entity.OperationLog;
import com.wireless.model.entity.VoiceLog;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 日志查询接口
 */
@Tag(name = "日志查询", description = "语音记录、操作记录、告警记录")
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogController {

    private final VoiceLogMapper voiceLogMapper;
    private final OperationLogMapper operationLogMapper;
    private final AlertService alertService;

    @Operation(summary = "查询语音操作日志")
    @GetMapping("/voice")
    public ApiResult<List<VoiceLog>> getVoiceLogs() {
        return ApiResult.success(voiceLogMapper.selectAll());
    }

    @Operation(summary = "查询设备操作日志")
    @GetMapping("/operation")
    public ApiResult<List<OperationLog>> getOperationLogs(
            @RequestParam(required = false) String deviceCode) {
        if (deviceCode != null) {
            return ApiResult.success(operationLogMapper.selectByDeviceCode(deviceCode));
        }
        return ApiResult.success(operationLogMapper.selectByDeviceCode("led")); // 默认返回全部
    }

    @Operation(summary = "查询告警日志")
    @GetMapping("/alert")
    public ApiResult<List<AlertLog>> getAlertLogs(
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return ApiResult.success(alertService.getAlerts(alertType, startTime, endTime));
    }

    @Operation(summary = "标记告警已处理")
    @PutMapping("/alert/{id}/handle")
    public ApiResult<?> handleAlert(@PathVariable Long id) {
        alertService.markHandled(id);
        return ApiResult.success();
    }
}
