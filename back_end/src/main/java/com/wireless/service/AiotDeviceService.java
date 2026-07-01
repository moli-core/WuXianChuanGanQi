package com.wireless.service;

import com.wireless.model.entity.AiotDevice;
import com.wireless.model.vo.ApiResult;
import com.wireless.model.dto.DeviceRegisterRequest;

import java.util.List;
import java.util.Map;

public interface AiotDeviceService {
    List<AiotDevice> listDevices(String keyword, String deviceType, Integer onlineStatus);
    AiotDevice getByDeviceCode(String deviceCode);
    ApiResult<?> registerDevice(DeviceRegisterRequest request);
    ApiResult<?> updateDevice(Long id, DeviceRegisterRequest request);
    ApiResult<?> deleteDevice(Long id);

    /** 处理 ESP32 上报数据，自动注册或更新设备状态 */
    void handleDeviceReport(String deviceCode, Map<String, Object> reportData);

    /** 向设备下发指令 */
    ApiResult<?> sendCommand(String deviceCode, String command);

    /** 设备在线/离线状态更新 */
    void updateOnlineStatus(String deviceCode, boolean online);
}
