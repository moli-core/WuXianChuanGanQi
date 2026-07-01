package com.wireless.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 环境数据图表 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvChartVO {
    /** 时间轴 */
    private List<LocalDateTime> times;
    /** 温度数据 */
    private List<BigDecimal> temperatures;
    /** 湿度数据 */
    private List<BigDecimal> humidities;
    /** 烟雾浓度数据 */
    private List<BigDecimal> smokeLevels;
    /** 统计摘要 */
    private ChartSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartSummary {
        private BigDecimal avgTemp;
        private BigDecimal maxTemp;
        private BigDecimal minTemp;
        private BigDecimal avgHumidity;
        private BigDecimal maxSmoke;
    }
}
