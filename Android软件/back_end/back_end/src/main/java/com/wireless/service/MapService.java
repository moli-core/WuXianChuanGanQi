package com.wireless.service;

import java.util.Map;

public interface MapService {
    /** 路线规划 */
    Map<String, Object> getRoute(String origin, String destination);
    /** 地址搜索 */
    Map<String, Object> searchPlace(String keyword);
}
