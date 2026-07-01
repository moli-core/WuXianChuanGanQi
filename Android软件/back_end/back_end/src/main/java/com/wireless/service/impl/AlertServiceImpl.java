package com.wireless.service.impl;

import com.wireless.mapper.AlertLogMapper;
import com.wireless.model.entity.AlertLog;
import com.wireless.service.AlertService;
import com.wireless.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 告警服务实现 + 设备联动逻辑
 */
@Slf4j
@Service
public class AlertServiceImpl implements AlertService {

    private final AlertLogMapper alertLogMapper;
    private final DeviceService deviceService;

    /** 手动构造解决循环依赖: DeviceService → MqttService → AlertService → DeviceService */
    public AlertServiceImpl(AlertLogMapper alertLogMapper,
                            @Lazy DeviceService deviceService) {
        this.alertLogMapper = alertLogMapper;
        this.deviceService = deviceService;
    }

    @Value("${device.linkage.temp-high-threshold}")
    private Double tempHighThreshold;

    @Value("${device.linkage.smoke-alert-threshold}")
    private Double smokeAlertThreshold;

    @Override
    @Transactional
    public void checkAndAlert(Double temperature, Double humidity, Double smokeLevel) {
        if (temperature == null && humidity == null && smokeLevel == null) {
            return;
        }

        // ===== 1. 温度过高 → 自动开风扇 =====
        if (temperature != null && temperature > tempHighThreshold) {
            AlertLog alert = AlertLog.builder()
                    .alertType("temp_high")
                    .alertLevel(2)
                    .content(String.format("温度过高: %.1f°C，阈值 %.1f°C", temperature, tempHighThreshold))
                    .envValue(BigDecimal.valueOf(temperature))
                    .build();
            alertLogMapper.insert(alert);

            // 自动开启风扇
            deviceService.controlDevice("fan", 1, "auto");
            log.warn("⚠ 温度过高联动: 自动开启风扇 (temp={}°C)", temperature);
        }

        // ===== 2. 烟雾超标 → 触发蜂鸣器 =====
        if (smokeLevel != null && smokeLevel > smokeAlertThreshold) {
            AlertLog alert = AlertLog.builder()
                    .alertType("smoke_high")
                    .alertLevel(3)
                    .content(String.format("烟雾浓度超标: %.1f ppm，阈值 %.1f ppm", smokeLevel, smokeAlertThreshold))
                    .envValue(BigDecimal.valueOf(smokeLevel))
                    .build();
            alertLogMapper.insert(alert);

            // 触发蜂鸣器
            deviceService.controlDevice("buzzer", 1, "auto");
            log.error("⚠⚠ 烟雾超标联动: 触发蜂鸣器 (smoke={}ppm)", smokeLevel);
        }

        // ===== 3. 湿度异常记录（不触发设备） =====
        if (humidity != null && (humidity > 80 || humidity < 20)) {
            AlertLog alert = AlertLog.builder()
                    .alertType("humidity_abnormal")
                    .alertLevel(1)
                    .content(String.format("湿度异常: %.1f%%", humidity))
                    .envValue(BigDecimal.valueOf(humidity))
                    .build();
            alertLogMapper.insert(alert);
            log.warn("湿度异常记录: humidity={}%", humidity);
        }
    }

    @Override
    public List<AlertLog> getAlerts(String alertType, String startTime, String endTime) {
        if (startTime != null && endTime != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return alertLogMapper.selectByTimeRange(
                    LocalDateTime.parse(startTime, fmt),
                    LocalDateTime.parse(endTime, fmt)
            );
        }
        if (alertType != null && !alertType.isEmpty()) {
            return alertLogMapper.selectByType(alertType);
        }
        // 默认返回最近 100 条
        return alertLogMapper.selectByTimeRange(
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now()
        );
    }

    @Override
    public void markHandled(Long id) {
        alertLogMapper.markHandled(id);
    }
}
