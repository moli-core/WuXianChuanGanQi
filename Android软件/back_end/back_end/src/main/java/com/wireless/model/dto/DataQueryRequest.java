package com.wireless.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据查询请求
 */
@Data
public class DataQueryRequest {
    /** 开始时间 */
    private LocalDateTime startTime;
    /** 结束时间 */
    private LocalDateTime endTime;
    /** 查询范围 24h/7d/30d */
    private String range;
    /** 数据维度 temperature/humidity/smoke_level */
    private String dimension;
}
