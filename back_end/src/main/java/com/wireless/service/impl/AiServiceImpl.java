package com.wireless.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wireless.mapper.EnvDataMapper;
import com.wireless.model.entity.EnvData;
import com.wireless.service.AiService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AI 服务实现 (DeepSeek — 与硬件同学统一)
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final OkHttpClient okHttpClient;
    private final EnvDataMapper envDataMapper;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public AiServiceImpl(OkHttpClient okHttpClient,
                         EnvDataMapper envDataMapper,
                         @Value("${ai.api-key}") String apiKey,
                         @Value("${ai.base-url}") String baseUrl,
                         @Value("${ai.model}") String model) {
        this.okHttpClient = okHttpClient;
        this.envDataMapper = envDataMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.model = model;
    }

    @Override
    public String chat(String question, String sessionId) {
        EnvData latest = envDataMapper.selectLatest();
        String systemPrompt = "你是一个智能家居助手。";
        if (latest != null) {
            systemPrompt += String.format(
                    "当前环境: 温度 %.1f°C、湿度 %.1f%%、烟雾浓度 %.1f ppm。",
                    latest.getTemperature(), latest.getHumidity(), latest.getSmokeLevel()
            );
        }
        try {
            return callDeepSeek(systemPrompt, question, 0.7);
        } catch (Exception e) {
            log.error("DeepSeek 调用失败", e);
            return fallbackReply(question, latest);
        }
    }

    @Override
    public String composeSentence(Map<String, Object> request) {
        String systemPrompt = "你需要完成的工作是组词成句，必要情况下可以适当添加词语。回复我时只需要回复连好的句子。";

        // Support both {"text":"..."} and {"words":["...","..."]}
        String input = "";
        Object text = request.get("text");
        if (text instanceof String t && !t.isBlank()) {
            input = t.trim();
        }
        Object words = request.get("words");
        if (words instanceof List<?> wl && !wl.isEmpty()) {
            input = wl.stream().map(String::valueOf).map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .reduce((a, b) -> a + " " + b).orElse(input);
        }
        if (input.isEmpty()) {
            input = "你好";
        }

        try {
            return callDeepSeek(systemPrompt, input, 0.2);
        } catch (Exception e) {
            log.error("DeepSeek compose 失败", e);
            return input;
        }
    }

    private String callDeepSeek(String systemPrompt, String userMessage, double temperature) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing DEEPSEEK_API_KEY");
        }

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
        ));
        body.put("stream", false);
        body.put("max_tokens", 512);
        body.put("temperature", temperature);

        Request req = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toJSONString(), MediaType.parse("application/json")))
                .build();

        try (Response response = okHttpClient.newCall(req).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("DeepSeek HTTP " + response.code());
            }
            String respBody = response.body() != null ? response.body().string() : "";
            JSONObject result = JSON.parseObject(respBody);
            return result.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content").trim();
        }
    }

    private String fallbackReply(String question, EnvData env) {
        if (question.contains("温度") || question.contains("热")) {
            double t = env != null ? env.getTemperature().doubleValue() : 25;
            return t > 35 ? "当前温度 " + t + "°C，偏高，建议开启风扇。"
                    : t < 15 ? "当前温度 " + t + "°C，偏低，注意保暖。"
                    : "当前温度 " + t + "°C，舒适范围。";
        }
        if (question.contains("烟雾") || question.contains("火")) {
            double s = env != null ? env.getSmokeLevel().doubleValue() : 0;
            return s > 500 ? "警告：烟雾浓度 " + s + " ppm，严重超标！"
                    : "当前烟雾浓度 " + s + " ppm，正常。";
        }
        return "我是智能家居助手，可以帮您分析环境数据和控制设备。请问有什么需要？";
    }
}
