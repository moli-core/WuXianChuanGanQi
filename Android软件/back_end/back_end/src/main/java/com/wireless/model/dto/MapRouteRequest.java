package com.wireless.model.dto;

import lombok.Data;

/**
 * 地图路线规划请求
 */
@Data
public class MapRouteRequest {
    private String origin;          // 起点
    private String destination;     // 终点
    private String originCity;      // 起点城市
    private String destinationCity; // 终点城市
}
