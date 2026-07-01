package com.wireless.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wireless.model.entity.EnvData;
import com.wireless.service.AlertService;
import com.wireless.service.EnvDataService;
import com.wireless.service.MqttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * MQTT 通信服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttServiceImpl implements MqttService {

    private final MessageChannel mqttOutputChannel;
    private final EnvDataService envDataService;
    private final AlertService alertService;

    @Override
    public void publishCommand(String deviceCode, Integer action) {
        try {
            // 构造 JSON 指令: {"cmd":"led:on"} 或 {"cmd":"fan:off"}
            String command = deviceCode + ":" + (action == 1 ? "on" : "off");
            String payload = JSON.toJSONString(Map.of("cmd", command));

            Message<String> message = MessageBuilder
                    .withPayload(payload)
                    .setHeader(MqttHeaders.TOPIC, "default")
                    .build();
            mqttOutputChannel.send(message);

            log.info("MQTT 指令已发布: {}", payload);
        } catch (Exception e) {
            log.error("MQTT 发布失败: device={}, action={}", deviceCode, action, e);
        }
    }

    @Override
    public void handleDeviceReport(String topic, String payload) {
        try {
            log.debug("MQTT 收到上报: topic={}, payload={}", topic, payload);

            // 解析 ESP32 上报的 JSON 数据 (与硬件同学 ESP32 字段名统一)
            // 格式: {"Data_temp":26,"Data_humi":62,"Status_LED":true,"Status_body":0,"Status_beeper":false,...}
            JSONObject data = JSON.parseObject(payload);

            // 保存环境数据
            EnvData envData = EnvData.builder()
                    .temperature(data.getBigDecimal("Data_temp"))
                    .humidity(data.getBigDecimal("Data_humi"))
                    .smokeLevel(data.getBigDecimal("smoke"))
                    .build();
            envDataService.saveEnvData(envData);

            // 触发设备联动检查
            alertService.checkAndAlert(
                    envData.getTemperature() != null ? envData.getTemperature().doubleValue() : null,
                    envData.getHumidity() != null ? envData.getHumidity().doubleValue() : null,
                    envData.getSmokeLevel() != null ? envData.getSmokeLevel().doubleValue() : null
            );

        } catch (Exception e) {
            log.error("MQTT 数据处理失败: payload={}", payload, e);
        }
    }
}
