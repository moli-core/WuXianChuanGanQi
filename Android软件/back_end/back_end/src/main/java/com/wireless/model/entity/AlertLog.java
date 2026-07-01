package com.wireless.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 异常告警日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertLog {
    private Long id;
    private String alertType;     // temp_high/smoke_high/humidity_abnormal
    private Integer alertLevel;   // 1-普通 2-严重 3-紧急
    private String content;
    private BigDecimal envValue;
    private Integer isHandled;    // 0-未处理 1-已处理
    private LocalDateTime createTime;
}
