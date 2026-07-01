package com.wireless.handler;

import com.wireless.service.AlertService;
import com.wireless.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 设备自动联动逻辑处理器
 *
 * 联动规则:
 * 1. 温度 > 35°C → 自动开风扇
 * 2. 烟雾 > 500ppm → 触发蜂鸣器 + 告警
 * 3. 湿度异常 → 记录告警
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceLinkageHandler {

    private final AlertService alertService;
    private final DeviceService deviceService;

    /**
     * 联动入口 — 由 MQTT 消息处理时调用
     */
    public void executeLinkage(Double temperature, Double humidity, Double smokeLevel) {
        log.debug("设备联动检查: temp={}, humidity={}, smoke={}", temperature, humidity, smokeLevel);
        alertService.checkAndAlert(temperature, humidity, smokeLevel);
    }
}
