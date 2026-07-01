package com.wireless.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 设备状态实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatus {
    private Integer id;
    private String deviceCode;
    private String deviceName;
    private Integer status;       // 0-关 1-开
    private LocalDateTime updateTime;
}
