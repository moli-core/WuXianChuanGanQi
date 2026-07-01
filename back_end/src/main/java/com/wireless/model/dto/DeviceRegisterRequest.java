package com.wireless.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 设备注册请求
 */
@Data
public class DeviceRegisterRequest {
    @NotBlank
    private String deviceCode;
    private String deviceName;
    private String deviceType;
    private String ipAddress;
    private String macAddress;
    private String mqttTopic;
    private String sensorList;
    private String remarks;
}
