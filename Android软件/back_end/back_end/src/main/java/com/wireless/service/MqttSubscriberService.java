package com.wireless.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wireless.mapper.AiotDeviceMapper;
import com.wireless.mapper.EnvDataMapper;
import com.wireless.model.entity.AiotDevice;
import com.wireless.model.entity.EnvData;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * 华为 IoT MQTT 订阅服务 — 接收 ESP32 上报数据
 */
@Slf4j
@Service
public class MqttSubscriberService {

    private final EnvDataMapper envDataMapper;
    private final AiotDeviceMapper aiotDeviceMapper;
    private final AlertService alertService;
    private MqttClient client;

    private static final String BROKER = "tcp://d1aaeb8e71.st1.iotda-device.cn-south-1.myhuaweicloud.com:1883";
    private static final String CLIENT_ID = "6a3e4b21c00ccb6d4b6000c5_backend_sub";  // 跟 ESP32 不同即可
    private static final String USERNAME = "6a3e4b21c00ccb6d4b6000c5_Esp32_Device";
    private static final String PASSWORD = "e1afac7e15ca83eca6d5d5da214088a8133e4fa8c86e7c1caec0ee72d46cb048";  // 跟 ESP32 完全一样
    private static final String TOPIC_REPORT = "$oc/devices/6a3e4b21c00ccb6d4b6000c5_Esp32_Device/sys/properties/report";

    public MqttSubscriberService(EnvDataMapper envDataMapper,
                                  AiotDeviceMapper aiotDeviceMapper,
                                  AlertService alertService) {
        this.envDataMapper = envDataMapper;
        this.aiotDeviceMapper = aiotDeviceMapper;
        this.alertService = alertService;
    }

    @PostConstruct
    public void connect() {
        try {
            client = new MqttClient(BROKER, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(USERNAME);
            options.setPassword(PASSWORD.toCharArray());
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setKeepAliveInterval(60);
            options.setConnectionTimeout(10);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("MQTT 断开, 重连中...");
                }
                @Override
                public void messageArrived(String topic, MqttMessage msg) {
                    handleMessage(topic, new String(msg.getPayload(), StandardCharsets.UTF_8));
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            client.connect(options);
            client.subscribe(TOPIC_REPORT, 1);
            log.info("MQTT 订阅成功: {} -> {}", BROKER, TOPIC_REPORT);
        } catch (Exception e) {
            log.error("MQTT 订阅连接失败: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (Exception ignored) {}
    }

    private void handleMessage(String topic, String payload) {
        try {
            log.debug("MQTT 收到: {}", payload);
            JSONObject root = JSON.parseObject(payload);

            // 解析 services[0].properties
            JSONObject props = root;
            if (root.containsKey("services")) {
                var services = root.getJSONArray("services");
                if (services != null && !services.isEmpty()) {
                    props = services.getJSONObject(0).getJSONObject("properties");
                }
            }
            if (props == null) return;

            // 保存环境数据
            EnvData env = EnvData.builder()
                    .deviceCode("esp32_001")
                    .temperature(props.getBigDecimal("Data_temp"))
                    .humidity(props.getBigDecimal("Data_humi"))
                    .build();
            envDataMapper.insert(env);

            // 更新设备实时状态
            AiotDevice device = AiotDevice.builder()
                    .deviceCode("esp32_001")
                    .deviceName("ESP32设备")
                    .ledStatus(boolToInt(props, "Status_LED"))
                    .mode((String) null) // ESP32 无模式字段时保留
                    .pirStatus(boolToInt(props, "Status_body"))
                    .onlineStatus(1)
                    .build();
            aiotDeviceMapper.upsertOnReport(device);

            // 联动检查
            Double temp = props.getDouble("Data_temp");
            Double humi = props.getDouble("Data_humi");
            if (temp != null || humi != null) {
                alertService.checkAndAlert(temp, humi, null);
            }

        } catch (Exception e) {
            log.error("MQTT 消息解析失败: {}", payload, e);
        }
    }

    private Integer boolToInt(JSONObject obj, String key) {
        Boolean b = obj.getBoolean(key);
        if (b != null) return b ? 1 : 0;
        Integer i = obj.getInteger(key);
        return i != null ? (i != 0 ? 1 : 0) : null;
    }
}
