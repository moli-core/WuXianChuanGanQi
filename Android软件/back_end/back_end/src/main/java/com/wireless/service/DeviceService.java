package com.wireless.service;

import com.wireless.model.entity.DeviceStatus;
import java.util.List;

/**
 * 设备控制服务
 */
public interface DeviceService {

    /** 查询所有设备状态 */
    List<DeviceStatus> getAllStatus();

    /** 查询单个设备状态 */
    DeviceStatus getStatus(String deviceCode);

    /** 控制设备 (手动) */
    void controlDevice(String deviceCode, Integer action, String source);

    /** 全开设备 */
    void turnAllOn();

    /** 全关设备 */
    void turnAllOff();
}
