package com.wireless.handler;

import com.wireless.service.MqttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * MQTT 消息处理器 — 处理 ESP32 上报数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttMessageHandler {

    private final MqttService mqttService;
    private final DeviceLinkageHandler deviceLinkageHandler;

    /**
     * 处理从 MQTT 入站通道收到的设备上报消息
     */
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<?> message) {
        try {
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            String payload = (String) message.getPayload();

            log.debug("MQTT 入站: topic={}, payload={}", topic, payload);

            if (payload != null && !payload.isEmpty()) {
                // 交给 MqttService 解析并持久化
                mqttService.handleDeviceReport(topic, payload);
            }
        } catch (Exception e) {
            log.error("MQTT 消息处理异常", e);
        }
    }
}
