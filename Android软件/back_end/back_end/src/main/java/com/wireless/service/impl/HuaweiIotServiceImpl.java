package com.wireless.service.impl;

import com.alibaba.fastjson2.JSON;
import com.wireless.service.HuaweiIotService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 华为 IoTDA 云平台服务 — 向 ESP32 设备下发属性
 * 与硬件同学后端统一: service_id=Esp32, properties={Status_LED: true/false}
 */
@Slf4j
@Service
public class HuaweiIotServiceImpl implements HuaweiIotService {

    private final OkHttpClient httpClient;
    private final String region;
    private final String iamEndpoint;
    private final String iotdaEndpoint;
    private final String projectId;
    private final String instanceId;
    private final String deviceId;
    private final String serviceId;
    private final String userName;
    private final String password;
    private final String domainName;

    private String token;
    private Instant tokenExpiresAt = Instant.EPOCH;
    private String tokenProjectId;

    public HuaweiIotServiceImpl(OkHttpClient okHttpClient,
                                 @Value("${huawei.iot.region}") String region,
                                 @Value("${huawei.iot.iam-endpoint}") String iamEndpoint,
                                 @Value("${huawei.iot.iotda-endpoint}") String iotdaEndpoint,
                                 @Value("${huawei.iot.project-id:}") String projectId,
                                 @Value("${huawei.iot.instance-id:}") String instanceId,
                                 @Value("${huawei.iot.device-id}") String deviceId,
                                 @Value("${huawei.iot.service-id}") String serviceId,
                                 @Value("${huawei.iot.auth-username}") String userName,
                                 @Value("${huawei.iot.auth-password}") String password,
                                 @Value("${huawei.iot.auth-domain}") String domainName) {
        this.httpClient = okHttpClient;
        this.region = region;
        this.iamEndpoint = iamEndpoint.replaceAll("/$", "");
        this.iotdaEndpoint = iotdaEndpoint.replaceAll("/$", "");
        this.projectId = projectId;
        this.instanceId = instanceId;
        this.deviceId = deviceId;
        this.serviceId = serviceId != null && !serviceId.isBlank() ? serviceId : "Esp32";
        this.userName = userName;
        this.password = password;
        this.domainName = domainName;
    }

    @Override
    public void sendProperties(Map<String, Object> command) throws Exception {
        String svcId = String.valueOf(command.getOrDefault("service_id", serviceId));
        Object properties = command.get("properties");
        if (!(properties instanceof Map<?, ?> pm) || pm.isEmpty()) {
            return;
        }

        String token = ensureToken(false);
        String url = iotdaEndpoint + "/v5/iot/" + effectiveProjectId() + "/devices/" + deviceId + "/properties";

        String body = JSON.toJSONString(Map.of(
                "services", List.of(Map.of("service_id", svcId, "properties", pm))
        ));

        Request.Builder builder = new Request.Builder()
                .url(url)
                .put(RequestBody.create(body, MediaType.parse("application/json")))
                .header("Content-Type", "application/json")
                .header("X-Auth-Token", token);
        if (instanceId != null && !instanceId.isBlank()) {
            builder.header("Instance-Id", instanceId);
        }

        try (Response response = httpClient.newCall(builder.build()).execute()) {
            if (response.code() == 401) {
                token = ensureToken(true);
                builder.header("X-Auth-Token", token);
                try (Response retryResp = httpClient.newCall(builder.build()).execute()) {
                    if (!retryResp.isSuccessful()) {
                        throw new IOException("Huawei IoT HTTP " + retryResp.code() + ": " + retryResp.body().string());
                    }
                }
            } else if (!response.isSuccessful()) {
                throw new IOException("Huawei IoT HTTP " + response.code() + ": " + response.body().string());
            }
        }
        log.info("华为 IoT 属性下发成功: {}", body);
    }

    @Override
    public void sendLed(boolean enabled) throws Exception {
        sendProperties(ledCommand(enabled));
    }

    @Override
    public Map<String, Object> ledCommand(boolean enabled) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("Status_LED", enabled);
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("service_id", serviceId);
        command.put("properties", properties);
        command.put("intent", enabled ? "turn_on_led" : "turn_off_led");
        return command;
    }

    @Override
    public Map<String, Object> sendVoiceCommand(String voiceText) throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("voiceText", voiceText);

        // 本地关键词快控
        Map<String, Object> command = keywordMatch(voiceText);
        // TODO: 复杂语义走 DeepSeek 大模型兜底
        result.put("controlCmd", command);
        sendProperties(command);
        result.put("cloudSent", true);
        return result;
    }

    /** 本地关键词匹配 (快控优先) */
    private Map<String, Object> keywordMatch(String text) {
        boolean ledOn = text.contains("打开") || text.contains("开启") || text.contains("开灯") || text.contains("亮灯");
        boolean ledOff = text.contains("关闭") || text.contains("关灯") || text.contains("熄灯");

        Map<String, Object> properties = new LinkedHashMap<>();
        String intent = "unknown";
        if (ledOn) {
            properties.put("Status_LED", true);
            intent = "turn_on_led";
        } else if (ledOff) {
            properties.put("Status_LED", false);
            intent = "turn_off_led";
        }

        Map<String, Object> command = new LinkedHashMap<>();
        command.put("service_id", serviceId);
        command.put("properties", properties);
        command.put("intent", intent);
        return command;
    }

    private synchronized String ensureToken(boolean forceRefresh) throws IOException {
        if (!forceRefresh && token != null && Instant.now().isBefore(tokenExpiresAt.minus(Duration.ofMinutes(5)))) {
            return token;
        }

        String body = JSON.toJSONString(Map.of(
                "auth", Map.of(
                        "identity", Map.of(
                                "methods", List.of("password"),
                                "password", Map.of(
                                        "user", Map.of(
                                                "name", userName,
                                                "password", password,
                                                "domain", Map.of("name", domainName)
                                        )
                                )
                        ),
                        "scope", Map.of("project", Map.of("name", region))
                )
        ));

        Request request = new Request.Builder()
                .url(iamEndpoint + "/v3/auth/tokens?nocatalog=true")
                .post(RequestBody.create(body, MediaType.parse("application/json;charset=utf8")))
                .header("Content-Type", "application/json;charset=utf8")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Huawei IAM HTTP " + response.code());
            }
            token = response.header("X-Subject-Token");
            if (token == null) {
                token = response.header("x-subject-token");
            }
            if (token == null) {
                throw new IOException("Huawei IAM response has no X-Subject-Token");
            }
            tokenExpiresAt = Instant.now().plus(Duration.ofHours(12));

            // 从响应体解析 project_id (华为 IoT API 必需)
            String respBody = response.body() != null ? response.body().string() : "";
            if (!respBody.isBlank()) {
                var root = JSON.parseObject(respBody);
                var tokenInfo = root.getJSONObject("token");
                if (tokenInfo != null) {
                    var project = tokenInfo.getJSONObject("project");
                    if (project != null && project.getString("id") != null) {
                        tokenProjectId = project.getString("id");
                    }
                }
            }
        }
        return token;
    }

    private String effectiveProjectId() {
        if (projectId != null && !projectId.isBlank()) return projectId;
        if (tokenProjectId != null && !tokenProjectId.isBlank()) return tokenProjectId;
        throw new IllegalStateException("Missing HW_PROJECT_ID and token project id");
    }

    /** 获取 IAM Token (供 DeviceShadowPoller 等使用) */
    public String getToken() {
        try { return ensureToken(false); } catch (Exception e) { return null; }
    }

    /** 获取 project_id (供 DeviceShadowPoller 等使用) */
    public String getProjectId() { return effectiveProjectId(); }
}
