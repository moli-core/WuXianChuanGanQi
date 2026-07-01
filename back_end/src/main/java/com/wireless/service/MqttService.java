package com.wireless.service;

/**
 * MQTT 通信服务
 */
public interface MqttService {

    /** 向设备下发指令 */
    void publishCommand(String deviceCode, Integer action);

    /** 处理设备上报数据 */
    void handleDeviceReport(String topic, String payload);
}
