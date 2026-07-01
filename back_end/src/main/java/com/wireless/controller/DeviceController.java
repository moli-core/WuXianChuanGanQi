package com.wireless.controller;

import com.wireless.model.dto.DeviceControlRequest;
import com.wireless.model.entity.DeviceStatus;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.DeviceService;
import com.wireless.service.impl.DeviceServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设备手动控制接口
 */
@Tag(name = "设备控制", description = "手动开关设备、查询状态")
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceServiceImpl deviceServiceImpl;

    @Operation(summary = "查询所有设备状态")
    @GetMapping("/status")
    public ApiResult<List<DeviceStatus>> getAllStatus() {
        return ApiResult.success(deviceService.getAllStatus());
    }

    @Operation(summary = "查询单个设备状态")
    @GetMapping("/status/{deviceCode}")
    public ApiResult<DeviceStatus> getStatus(@PathVariable String deviceCode) {
        DeviceStatus status = deviceService.getStatus(deviceCode);
        if (status == null) {
            return ApiResult.error("设备不存在: " + deviceCode);
        }
        return ApiResult.success(status);
    }

    @Operation(summary = "控制设备开关")
    @PostMapping("/control")
    public ApiResult<?> controlDevice(@Valid @RequestBody DeviceControlRequest request) {
        String source = request.getSource() != null ? request.getSource() : "manual";
        deviceService.controlDevice(request.getDeviceCode(), request.getAction(), source);
        return ApiResult.success();
    }

    @Operation(summary = "控制灯光 (兼容硬件同学 POST /api/device/light)")
    @PostMapping("/light")
    public ApiResult<Map<String, Object>> controlLight(@RequestBody Map<String, Object> request) {
        Object enabled = request.get("enabled");
        if (!(enabled instanceof Boolean)) {
            return ApiResult.badRequest("enabled must be boolean");
        }
        Map<String, Object> result = deviceServiceImpl.controlLight((Boolean) enabled, "manual");
        return ApiResult.success(result);
    }

    @Operation(summary = "一键全开")
    @PostMapping("/all-on")
    public ApiResult<?> turnAllOn() {
        deviceService.turnAllOn();
        return ApiResult.success();
    }

    @Operation(summary = "一键全关")
    @PostMapping("/all-off")
    public ApiResult<?> turnAllOff() {
        deviceService.turnAllOff();
        return ApiResult.success();
    }
}
