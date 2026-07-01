package com.wireless.controller;

import com.wireless.model.dto.DeviceRegisterRequest;
import com.wireless.model.entity.AiotDevice;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.AiotDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AIOT 设备管理", description = "设备 CRUD、状态查询、指令下发")
@RestController
@RequestMapping("/api/aiot-device")
@RequiredArgsConstructor
public class AiotDeviceController {

    private final AiotDeviceService aiotDeviceService;

    @Operation(summary = "设备列表 (支持筛选)")
    @GetMapping
    public ApiResult<List<AiotDevice>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) Integer onlineStatus) {
        return ApiResult.success(aiotDeviceService.listDevices(keyword, deviceType, onlineStatus));
    }

    @Operation(summary = "设备详情")
    @GetMapping("/{deviceCode}")
    public ApiResult<AiotDevice> detail(@PathVariable String deviceCode) {
        AiotDevice device = aiotDeviceService.getByDeviceCode(deviceCode);
        return device != null ? ApiResult.success(device) : ApiResult.error("设备不存在");
    }

    @Operation(summary = "注册新设备")
    @PostMapping
    public ApiResult<?> register(@Valid @RequestBody DeviceRegisterRequest request) {
        return aiotDeviceService.registerDevice(request);
    }

    @Operation(summary = "修改设备信息")
    @PutMapping("/{id}")
    public ApiResult<?> update(@PathVariable Long id, @RequestBody DeviceRegisterRequest request) {
        return aiotDeviceService.updateDevice(id, request);
    }

    @Operation(summary = "删除设备")
    @DeleteMapping("/{id}")
    public ApiResult<?> delete(@PathVariable Long id) {
        return aiotDeviceService.deleteDevice(id);
    }

    @Operation(summary = "下发设备指令")
    @PostMapping("/{deviceCode}/command")
    public ApiResult<?> sendCommand(@PathVariable String deviceCode,
                                    @RequestParam String command) {
        return aiotDeviceService.sendCommand(deviceCode, command);
    }
}
