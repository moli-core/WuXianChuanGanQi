package com.wireless.service;

import com.wireless.model.entity.AlertLog;
import java.util.List;

/**
 * 告警服务
 */
public interface AlertService {

    /** 检查环境数据并触发告警 */
    void checkAndAlert(Double temperature, Double humidity, Double smokeLevel);

    /** 查询告警列表 */
    List<AlertLog> getAlerts(String alertType, String startTime, String endTime);

    /** 标记告警已处理 */
    void markHandled(Long id);
}
