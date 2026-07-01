package com.wireless.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wireless.service.AiService;
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
    private final AiService aiService;
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
                                 AiService aiService,
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
        this.aiService = aiService;
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

        // ① 本地关键词快控
        Map<String, Object> command = keywordMatch(voiceText);

        // ② DeepSeek 大模型兜底 (本地匹配不到时)
        String intent = (String) command.get("intent");
        if ("unknown".equals(intent)) {
            Map<String, Object> aiCommand = deepSeekFallback(voiceText);
            if (aiCommand != null) {
                command = aiCommand;
                result.put("parseMethod", "deepseek");
            } else {
                result.put("parseMethod", "local_unknown");
            }
        } else {
            result.put("parseMethod", "local");
        }

        result.put("controlCmd", command);
        sendProperties(command);
        result.put("cloudSent", true);
        return result;
    }

    /** ① 本地关键词匹配 (快控优先 — 覆盖所有设备) */
    private Map<String, Object> keywordMatch(String text) {
        Map<String, Object> properties = new LinkedHashMap<>();
        String intent = "unknown";

        String lower = text.toLowerCase();
        boolean on = lower.contains("开") || lower.contains("打开") || lower.contains("开启")
                  || lower.contains("亮") || lower.contains("on");
        boolean off = lower.contains("关") || lower.contains("关闭") || lower.contains("关掉")
                   || lower.contains("灭") || lower.contains("熄") || lower.contains("off");

        // 绿灯
        if (lower.contains("绿") || lower.contains("灯") && !lower.contains("红") && !lower.contains("黄")) {
            if (on) { properties.put("Status_LED", true); intent = "turn_on_led"; }
            else if (off) { properties.put("Status_LED", false); intent = "turn_off_led"; }
        }
        // 红灯
        if (lower.contains("红")) {
            if (on) { properties.put("Status_ledRed", true); intent = "turn_on_ledRed"; }
            else if (off) { properties.put("Status_ledRed", false); intent = "turn_off_ledRed"; }
        }
        // 黄灯
        if (lower.contains("黄")) {
            if (on) { properties.put("Status_ledYellow", true); intent = "turn_on_ledYellow"; }
            else if (off) { properties.put("Status_ledYellow", false); intent = "turn_off_ledYellow"; }
        }
        // 蜂鸣器
        if (lower.contains("蜂鸣") || lower.contains("警报") || lower.contains("报警")) {
            if (on) { properties.put("Status_beeper", true); intent = "turn_on_beeper"; }
            else if (off) { properties.put("Status_beeper", false); intent = "turn_off_beeper"; }
        }
        // 全部
        if (lower.contains("全部") || lower.contains("所有") || lower.contains("一键")) {
            if (on) {
                properties.put("Status_LED", true);
                properties.put("Status_ledRed", true);
                properties.put("Status_ledYellow", true);
                properties.put("Status_beeper", true);
                intent = "turn_all_on";
            } else if (off) {
                properties.put("Status_LED", false);
                properties.put("Status_ledRed", false);
                properties.put("Status_ledYellow", false);
                properties.put("Status_beeper", false);
                intent = "turn_all_off";
            }
        }

        Map<String, Object> command = new LinkedHashMap<>();
        command.put("service_id", serviceId);
        command.put("properties", properties);
        command.put("intent", intent);
        return command;
    }

    /** ② DeepSeek 大模型兜底 — 复杂语义理解 */
    private Map<String, Object> deepSeekFallback(String voiceText) {
        try {
            String prompt = "你是智能家居语音助手。分析用户意图，返回JSON控制指令。" +
                "可用设备: Status_LED(绿灯), Status_ledRed(红灯), Status_ledYellow(黄灯), Status_beeper(蜂鸣器)。" +
                "全是bool, true开false关。也可以多个设备同时控制。" +
                "用户说: \"" + voiceText + "\"。只返回JSON: {\"properties\":{\"Status_LED\":true},\"intent\":\"...\"}，不要解释。";

            String reply = aiService.chat(prompt, null);
            if (reply == null || reply.isBlank()) return null;

            // 从 AI 回复中提取 JSON
            int start = reply.indexOf('{');
            int end = reply.lastIndexOf('}');
            if (start < 0 || end < start) return null;
            String jsonStr = reply.substring(start, end + 1);

            JSONObject parsed = JSON.parseObject(jsonStr);
            Map<String, Object> cmd = new LinkedHashMap<>();
            cmd.put("service_id", serviceId);
            cmd.put("properties", parsed.getJSONObject("properties"));
            cmd.put("intent", parsed.getString("intent"));
            log.info("DeepSeek 兜底解析: \"{}\" → {}", voiceText, JSON.toJSONString(cmd));
            return cmd;
        } catch (Exception e) {
            log.warn("DeepSeek 兜底失败: {}", e.getMessage());
            return null;
        }
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
