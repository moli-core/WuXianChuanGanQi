package com.wireless.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 设备操作日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {
    private Long id;
    private String deviceCode;
    private String deviceName;
    private Integer action;       // 0-关 1-开
    private String source;        // voice/manual/auto
    private LocalDateTime createTime;
}
