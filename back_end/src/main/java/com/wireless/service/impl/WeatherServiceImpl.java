package com.wireless.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.wireless.service.WeatherService;
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

/**
 * 天气服务实现 (心知天气 API)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherServiceImpl implements WeatherService {

    private final OkHttpClient okHttpClient;

    @Value("${weather.api-key}")
    private String apiKey;

    @Value("${weather.api-url}")
    private String apiUrl;

    @Value("${weather.city}")
    private String city;

    @Override
    public Map<String, Object> getCurrentWeather(String cityParam) {
        String targetCity = (cityParam != null && !cityParam.isBlank()) ? cityParam : city;
        Map<String, Object> result = new HashMap<>();
        try {
            String url = apiUrl + "?key=" + apiKey + "&location=" + targetCity;
            Request request = new Request.Builder().url(url).build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    JSONObject json = JSON.parseObject(body);
                    JSONObject results = json.getJSONArray("results").getJSONObject(0);
                    JSONObject now = results.getJSONObject("now");

                    result.put("city", results.getJSONObject("location").getString("name"));
                    result.put("temperature", now.getString("temperature"));
                    result.put("weather", now.getString("text"));
                    result.put("humidity", now.getString("humidity"));
                    result.put("windDirection", now.getString("wind_direction"));
                    result.put("windSpeed", now.getString("wind_speed"));
                    result.put("updateTime", results.getString("last_update"));
                    return result;
                }
            }
        } catch (IOException e) {
            log.error("天气 API 调用失败", e);
        }

        // 降级返回
        result.put("city", city);
        result.put("message", "天气数据获取失败");
        return result;
    }

    @Override
    public String getCity() {
        return city;
    }
}
