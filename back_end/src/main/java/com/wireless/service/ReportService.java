package com.wireless.service;

import java.util.Map;

public interface ReportService {
    /** 设备总览统计 */
    Map<String, Object> getDeviceStats();

    /** 事件趋势 (折线图) */
    Map<String, Object> getEventTrends(int days);

    /** 事件类型分布 (环图) */
    Map<String, Object> getEventDistribution();

    /** 设备活跃排行 (柱状图) */
    Map<String, Object> getDeviceActivityRanking(int limit);

    /** 消息历史 */
    Map<String, Object> getMessageHistory(int page, int pageSize);
}
