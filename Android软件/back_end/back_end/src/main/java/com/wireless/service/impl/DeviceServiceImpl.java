package com.wireless.service.impl;

import com.wireless.handler.DeviceWebSocketHandler;
import com.wireless.mapper.DeviceStatusMapper;
import com.wireless.mapper.OperationLogMapper;
import com.wireless.model.entity.DeviceStatus;
import com.wireless.model.entity.OperationLog;
import com.wireless.service.DeviceService;
import com.wireless.service.HuaweiIotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备控制服务 — 华为 IoT 云平台版
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceStatusMapper deviceStatusMapper;
    private final OperationLogMapper operationLogMapper;
    private final HuaweiIotService huaweiIotService;

    @Override
    public List<DeviceStatus> getAllStatus() {
        return deviceStatusMapper.selectAll();
    }

    @Override
    public DeviceStatus getStatus(String deviceCode) {
        return deviceStatusMapper.selectByDeviceCode(deviceCode);
    }

    @Override
    @Transactional
    public void controlDevice(String deviceCode, Integer action, String source) {
        // 1. 更新数据库
        deviceStatusMapper.updateStatus(deviceCode, action);
        DeviceStatus device = deviceStatusMapper.selectByDeviceCode(deviceCode);

        // 2. 记录日志
        OperationLog logEntry = OperationLog.builder()
                .deviceCode(deviceCode)
                .deviceName(device != null ? device.getDeviceName() : deviceCode)
                .action(action)
                .source(source)
                .build();
        operationLogMapper.insert(logEntry);

        // 3. 华为 IoT 云平台下发 (与硬件同学统一)
        try {
            // 华为 IoT 属性名映射 (必须与 ESP32 代码一致)
            String propertyName = switch (deviceCode) {
                case "led"       -> "Status_LED";       // 绿灯
                case "ledRed"    -> "Status_ledRed";     // 红灯
                case "ledYellow" -> "Status_ledYellow";  // 黄灯
                case "buzzer"    -> "Status_beeper";     // 蜂鸣器
                case "fan"       -> "Status_FAN";        // 风扇
                default          -> "Status_" + deviceCode;
            };
            Map<String, Object> props = new LinkedHashMap<>();
            props.put(propertyName, action == 1);
            Map<String, Object> cmd = new LinkedHashMap<>();
            cmd.put("service_id", "Esp32");
            cmd.put("properties", props);
            cmd.put("intent", action == 1 ? "turn_on_" + deviceCode : "turn_off_" + deviceCode);
            huaweiIotService.sendProperties(cmd);
        } catch (Exception e) {
            log.error("华为 IoT 下发失败: device={}, action={}", deviceCode, action, e);
        }

        log.info("设备控制: {} -> {} (来源: {})", deviceCode, action == 1 ? "开" : "关", source);

        // 4. WebSocket 推送状态 (用数字孪生能识别的格式)
        Map<String, Object> wsData = new LinkedHashMap<>();
        String propName = devicePropertyName(deviceCode);
        if (propName != null) {
            wsData.put(propName, action == 1);
            DeviceWebSocketHandler.broadcast(wsData);
        }
    }

    /** 兼容硬件同学的 POST /api/device/light */
    @Transactional
    public Map<String, Object> controlLight(boolean enabled, String source) {
        controlDevice("led", enabled ? 1 : 0, source);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("controlCmd", huaweiIotService.ledCommand(enabled));
        result.put("cloudSent", true);
        return result;
    }

    @Override
    @Transactional
    public void turnAllOn() {
        List<DeviceStatus> devices = deviceStatusMapper.selectAll();

        // 拼成一次华为 IoT 请求，全部设备同时开关
        Map<String, Object> props = new LinkedHashMap<>();
        for (DeviceStatus ds : devices) {
            String key = devicePropertyName(ds.getDeviceCode());
            if (key != null) props.put(key, true);
        }
        batchSend(props, 1);

        // 更新数据库 + 记录日志
        for (DeviceStatus ds : devices) {
            deviceStatusMapper.updateStatus(ds.getDeviceCode(), 1);
            operationLogMapper.insert(OperationLog.builder()
                    .deviceCode(ds.getDeviceCode()).deviceName(ds.getDeviceName())
                    .action(1).source("manual").build());
        }
    }

    @Override
    @Transactional
    public void turnAllOff() {
        List<DeviceStatus> devices = deviceStatusMapper.selectAll();

        Map<String, Object> props = new LinkedHashMap<>();
        for (DeviceStatus ds : devices) {
            String key = devicePropertyName(ds.getDeviceCode());
            if (key != null) props.put(key, false);
        }
        batchSend(props, 0);

        for (DeviceStatus ds : devices) {
            deviceStatusMapper.updateStatus(ds.getDeviceCode(), 0);
            operationLogMapper.insert(OperationLog.builder()
                    .deviceCode(ds.getDeviceCode()).deviceName(ds.getDeviceName())
                    .action(0).source("manual").build());
        }
    }

    private String devicePropertyName(String code) {
        return switch (code) {
            case "led"       -> "Status_LED";
            case "ledRed"    -> "Status_ledRed";
            case "ledYellow" -> "Status_ledYellow";
            case "buzzer"    -> "Status_beeper";
            default          -> null;  // fan 等不支持的跳过
        };
    }

    private void batchSend(Map<String, Object> properties, int action) {
        if (properties.isEmpty()) return;
        try {
            Map<String, Object> cmd = new LinkedHashMap<>();
            cmd.put("service_id", "Esp32");
            cmd.put("properties", properties);
            cmd.put("intent", action == 1 ? "turn_all_on" : "turn_all_off");
            huaweiIotService.sendProperties(cmd);
            log.info("批量下发: {} 个设备 -> {}", properties.size(), action == 1 ? "开" : "关");
        } catch (Exception e) {
            log.error("批量下发失败", e);
        }
    }
}
