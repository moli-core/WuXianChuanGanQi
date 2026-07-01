package com.wireless.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * AIOT 无线传感终端设备实体
 * 对应 ESP32 上报的全部字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiotDevice {
    private Long id;
    private String deviceCode;      // 设备唯一编码
    private String deviceName;      // 设备名称
    private String deviceType;      // esp32/sensor/gateway
    private String ipAddress;       // IP 地址
    private String macAddress;      // MAC 地址
    private String mqttTopic;       // MQTT 上报主题
    private String firmwareVersion; // 固件版本
    private String sensorList;      // 传感器清单 JSON

    // ESP32 上报的实时状态
    private Integer ledStatus;      // LED 0-关 1-开
    private String mode;            // auto/manual
    private Integer lightSensor;    // 光敏值
    private Integer pirStatus;      // 人体感应 0-无人 1-有人
    private Integer screenStatus;   // 屏幕 0-息屏 1-亮屏
    private Integer wifiRssi;       // WiFi 信号
    private Integer tcpConnected;   // TCP 连接

    // 管理字段
    private Integer onlineStatus;   // 0-离线 1-在线
    private LocalDateTime lastReportTime;
    private String registerSource;  // manual/auto
    private String remarks;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
