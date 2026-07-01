package com.wireless.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wireless.handler.DeviceWebSocketHandler;
import com.wireless.mapper.AiotDeviceMapper;
import com.wireless.mapper.DeviceStatusMapper;
import com.wireless.mapper.ServerEventMapper;
import com.wireless.model.dto.DeviceRegisterRequest;
import com.wireless.model.entity.AiotDevice;
import com.wireless.model.entity.ServerEvent;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.AiotDeviceService;
import com.wireless.service.MqttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiotDeviceServiceImpl implements AiotDeviceService {

    private final AiotDeviceMapper aiotDeviceMapper;
    private final DeviceStatusMapper deviceStatusMapper;
    private final ServerEventMapper serverEventMapper;
    private final MqttService mqttService;

    @Override
    public List<AiotDevice> listDevices(String keyword, String deviceType, Integer onlineStatus) {
        return aiotDeviceMapper.selectAll(keyword, deviceType, onlineStatus);
    }

    @Override
    public AiotDevice getByDeviceCode(String deviceCode) {
        return aiotDeviceMapper.selectByDeviceCode(deviceCode);
    }

    @Override
    public ApiResult<?> registerDevice(DeviceRegisterRequest request) {
        AiotDevice exist = aiotDeviceMapper.selectByDeviceCode(request.getDeviceCode());
        if (exist != null) {
            return ApiResult.error("设备编码已存在");
        }

        AiotDevice device = AiotDevice.builder()
                .deviceCode(request.getDeviceCode())
                .deviceName(request.getDeviceName())
                .deviceType(request.getDeviceType() != null ? request.getDeviceType() : "esp32")
                .ipAddress(request.getIpAddress())
                .macAddress(request.getMacAddress())
                .mqttTopic(request.getMqttTopic())
                .sensorList(request.getSensorList())
                .remarks(request.getRemarks())
                .registerSource("manual")
                .build();
        aiotDeviceMapper.insert(device);

        // 同步到 device_status 表（兼容旧接口）
        deviceStatusMapper.insertIfNotExists(
                com.wireless.model.entity.DeviceStatus.builder()
                        .deviceCode(request.getDeviceCode())
                        .deviceName(request.getDeviceName())
                        .status(0)
                        .build()
        );

        log.info("设备已注册: {}", request.getDeviceCode());
        return ApiResult.success(device);
    }

    @Override
    public ApiResult<?> updateDevice(Long id, DeviceRegisterRequest request) {
        AiotDevice device = AiotDevice.builder()
                .id(id)
                .deviceName(request.getDeviceName())
                .deviceType(request.getDeviceType())
                .ipAddress(request.getIpAddress())
                .macAddress(request.getMacAddress())
                .mqttTopic(request.getMqttTopic())
                .sensorList(request.getSensorList())
                .remarks(request.getRemarks())
                .build();
        aiotDeviceMapper.update(device);
        return ApiResult.success();
    }

    @Override
    @Transactional
    public ApiResult<?> deleteDevice(Long id) {
        aiotDeviceMapper.deleteById(id);
        return ApiResult.success();
    }

    @Override
    @Transactional
    public void handleDeviceReport(String deviceCode, Map<String, Object> reportData) {
        // 自动注册或更新设备
        AiotDevice device = AiotDevice.builder()
                .deviceCode(deviceCode)
                .deviceName(reportData.getOrDefault("deviceName", deviceCode).toString())
                .ipAddress((String) reportData.get("ip"))
                .macAddress((String) reportData.get("mac"))
                .mqttTopic((String) reportData.get("topic"))
                .ledStatus(parseInt(reportData.get("Status_LED")))
                .mode((String) reportData.getOrDefault("mode", "manual"))
                .lightSensor(parseInt(reportData.get("light_sensor")))
                .pirStatus(parseInt(reportData.get("Status_body")))
                .screenStatus(parseInt(reportData.get("screen")))
                .wifiRssi(parseInt(reportData.get("wifi_rssi")))
                .tcpConnected(parseInt(reportData.get("tcp")))
                .build();
        aiotDeviceMapper.upsertOnReport(device);

        // WebSocket 推送设备状态
        Map<String, Object> wsData = new HashMap<>(reportData);
        wsData.put("deviceCode", deviceCode);
        wsData.put("event", "device_report");
        DeviceWebSocketHandler.broadcast(wsData);
    }

    @Override
    public ApiResult<?> sendCommand(String deviceCode, String command) {
        AiotDevice device = aiotDeviceMapper.selectByDeviceCode(deviceCode);
        if (device == null) {
            return ApiResult.error("设备不存在");
        }
        if (device.getOnlineStatus() == 0) {
            return ApiResult.error("设备离线，无法下发指令");
        }

        // 解析指令并下发给 ESP32
        // command 格式: "led:on", "screen:off", "mode:auto"
        mqttService.publishCommand(deviceCode, "on".equals(command.split(":")[1]) ? 1 : 0);

        log.info("设备指令已下发: {} → {}", deviceCode, command);
        return ApiResult.success();
    }

    @Override
    public void updateOnlineStatus(String deviceCode, boolean online) {
        aiotDeviceMapper.updateOnlineStatus(deviceCode, online ? 1 : 0);
    }

    private Integer parseInt(Object val) {
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        try { return Integer.parseInt(val.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}
