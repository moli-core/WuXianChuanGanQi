package com.wireless.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 控制面板仪表盘 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO {
    /** 当前温度 */
    private BigDecimal currentTemp;
    /** 当前湿度 */
    private BigDecimal currentHumidity;
    /** 当前烟雾浓度 */
    private BigDecimal currentSmoke;
    /** 设备状态 Map<deviceCode, status> */
    private Map<String, Integer> deviceStatus;
    /** 今日告警次数 */
    private Integer todayAlertCount;
    /** 今日语音指令次数 */
    private Integer todayVoiceCount;
}
