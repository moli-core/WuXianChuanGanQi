package com.wireless.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 环境数据实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvData {
    private Long id;
    private String deviceCode;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private BigDecimal smokeLevel;
    private LocalDateTime createTime;
}
