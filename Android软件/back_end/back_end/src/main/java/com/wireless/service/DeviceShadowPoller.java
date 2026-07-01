package com.wireless.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wireless.handler.DeviceWebSocketHandler;
import com.wireless.mapper.AiotDeviceMapper;
import com.wireless.mapper.EnvDataMapper;
import com.wireless.model.entity.AiotDevice;
import com.wireless.model.entity.EnvData;
import com.wireless.service.impl.HuaweiIotServiceImpl;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 华为云设备影子轮询 — 定时拉取 ESP32 最新数据
 */
@Slf4j
@Service
public class DeviceShadowPoller {

    private final OkHttpClient httpClient;
    private final EnvDataMapper envDataMapper;
    private final AiotDeviceMapper aiotDeviceMapper;
    private final AlertService alertService;
    private final HuaweiIotServiceImpl huaweiIotService;
    private final String iotdaEndpoint;
    private final String deviceId;

    public DeviceShadowPoller(OkHttpClient httpClient,
                               EnvDataMapper envDataMapper,
                               AiotDeviceMapper aiotDeviceMapper,
                               AlertService alertService,
                               HuaweiIotServiceImpl huaweiIotService,
                               @Value("${huawei.iot.iotda-endpoint}") String iotdaEndpoint,
                               @Value("${huawei.iot.device-id}") String deviceId) {
        this.httpClient = httpClient;
        this.envDataMapper = envDataMapper;
        this.aiotDeviceMapper = aiotDeviceMapper;
        this.alertService = alertService;
        this.huaweiIotService = huaweiIotService;
        this.iotdaEndpoint = iotdaEndpoint.replaceAll("/$", "");
        this.deviceId = deviceId;
    }

    @PostConstruct
    public void init() {
        log.info("DeviceShadowPoller 已启动, 每5秒轮询华为云设备影子");
    }

    /** 每 5 秒拉一次设备影子 */
    @Scheduled(fixedRate = 5000)
    public void pollDeviceShadow() {
        log.info("设备影子轮询开始...");
        try {
            String token = huaweiIotService.getToken();
            if (token == null) { log.warn("设备影子轮询: IAM token 为空"); return; }

            String url = iotdaEndpoint + "/v5/iot/" + huaweiIotService.getProjectId() + "/devices/" + deviceId + "/shadow";
            log.info("请求设备影子: {}", url);
            Request req = new Request.Builder().url(url).header("X-Auth-Token", token).get().build();

            try (Response resp = httpClient.newCall(req).execute()) {
                log.info("设备影子 API 返回: {}", resp.code());
                if (!resp.isSuccessful()) {
                    log.warn("设备影子 API 返回错误: {} {}", resp.code(), resp.message());
                    return;
                }
                String body = resp.body() != null ? resp.body().string() : "";
                log.info("设备影子响应(前300字): {}", body.length() > 300 ? body.substring(0, 300) : body);
                parseAndSave(body);
                log.info("设备影子数据已保存");
            }
        } catch (Exception e) {
            log.error("设备影子轮询异常: {}", e.getMessage(), e);
        }
    }

    private void parseAndSave(String body) {
        JSONObject root = JSON.parseObject(body);
        if (root == null) return;

        // 设备影子格式: shadow[0].reported.properties = {...} (直接就是属性)
        var shadow = root.getJSONArray("shadow");
        if (shadow == null || shadow.isEmpty()) return;
        var reported = shadow.getJSONObject(0).getJSONObject("reported");
        if (reported == null) return;
        // reported.properties 直接就是 {Status_LED:true, Data_temp:26, ...}
        JSONObject p = reported.getJSONObject("properties");
        if (p == null) return;

        // 存环境数据
        EnvData env = EnvData.builder()
                .deviceCode(deviceId)
                .temperature(toDecimal(p, "Data_temp"))
                .humidity(toDecimal(p, "Data_humi"))
                .build();
        if (env.getTemperature() != null || env.getHumidity() != null) {
            envDataMapper.insert(env);
        }

        // 更新设备状态
        AiotDevice device = AiotDevice.builder()
                .deviceCode(deviceId)
                .deviceName("ESP32设备")
                .ledStatus(boolToInt(p, "Status_LED"))
                .pirStatus(boolToInt(p, "Status_body"))
                .onlineStatus(1)
                .build();
        aiotDeviceMapper.upsertOnReport(device);

        // 联动检查
        Double temp = toDouble(p, "Data_temp");
        Double humi = toDouble(p, "Data_humi");
        alertService.checkAndAlert(temp, humi, null);

        // 仅推送温湿度 (设备开关状态由 DeviceServiceImpl 即时推送，避免轮询覆盖用户操作)
        Map<String, Object> wsData = new LinkedHashMap<>();
        wsData.put("Data_temp", p.get("Data_temp"));
        wsData.put("Data_humi", p.get("Data_humi"));
        wsData.put("Status_body", p.get("Status_body"));
        DeviceWebSocketHandler.broadcast(wsData);

        log.info("设备影子保存: temp={}, humi={}, led={}", temp, humi, p.get("Status_LED"));
    }

    private java.math.BigDecimal toDecimal(JSONObject obj, String key) {
        try { return obj.getBigDecimal(key); } catch (Exception e) { return null; }
    }
    private Double toDouble(JSONObject obj, String key) {
        try { return obj.getDouble(key); } catch (Exception e) { return null; }
    }
    private Integer boolToInt(JSONObject obj, String key) {
        Boolean b = obj.getBoolean(key);
        return b == null ? null : (b ? 1 : 0);
    }

}
