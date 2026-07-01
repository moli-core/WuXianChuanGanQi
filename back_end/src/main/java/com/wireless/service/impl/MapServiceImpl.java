package com.wireless.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wireless.service.MapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {

    private final OkHttpClient okHttpClient;

    @Value("${map.api-key:}")
    private String apiKey;

    @Value("${map.api-url:https://restapi.amap.com/v3}")
    private String apiUrl;

    @Override
    public Map<String, Object> getRoute(String origin, String destination) {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = apiUrl + "/direction/driving?key=" + apiKey
                    + "&origin=" + origin + "&destination=" + destination + "&extensions=all";
            Request request = new Request.Builder().url(url).build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    result.put("data", JSON.parseObject(response.body().string()));
                    result.put("origin", origin);
                    result.put("destination", destination);
                    return result;
                }
            }
        } catch (IOException e) {
            log.error("地图 API 调用失败", e);
        }
        result.put("message", "路线规划失败，请检查地图 API Key");
        return result;
    }

    @Override
    public Map<String, Object> searchPlace(String keyword) {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = apiUrl + "/place/text?key=" + apiKey + "&keywords=" + keyword;
            Request request = new Request.Builder().url(url).build();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    result.put("data", JSON.parseObject(response.body().string()));
                    return result;
                }
            }
        } catch (IOException e) {
            log.error("地图搜索失败", e);
        }
        result.put("message", "搜索失败");
        return result;
    }
}
