package com.wireless.service;

import java.util.Map;

/**
 * 天气服务
 */
public interface WeatherService {

    /** 获取当前天气 */
    Map<String, Object> getCurrentWeather(String city);

    /** 获取城市 */
    String getCity();
}
