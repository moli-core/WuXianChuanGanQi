package com.wireless.service.impl;

import com.wireless.mapper.AlertLogMapper;
import com.wireless.mapper.DeviceStatusMapper;
import com.wireless.mapper.EnvDataMapper;
import com.wireless.mapper.VoiceLogMapper;
import com.wireless.model.entity.DeviceStatus;
import com.wireless.model.entity.EnvData;
import com.wireless.model.vo.DashboardVO;
import com.wireless.model.vo.EnvChartVO;
import com.wireless.service.EnvDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 环境数据服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnvDataServiceImpl implements EnvDataService {

    private final EnvDataMapper envDataMapper;
    private final DeviceStatusMapper deviceStatusMapper;
    private final AlertLogMapper alertLogMapper;
    private final VoiceLogMapper voiceLogMapper;

    @Override
    public void saveEnvData(EnvData envData) {
        envDataMapper.insert(envData);
        log.debug("环境数据已保存: temp={}, humidity={}, smoke={}",
                envData.getTemperature(), envData.getHumidity(), envData.getSmokeLevel());
    }

    @Override
    public EnvData getLatest() {
        return envDataMapper.selectLatest();
    }

    @Override
    public DashboardVO getDashboard() {
        EnvData latest = envDataMapper.selectLatest();
        List<DeviceStatus> devices = deviceStatusMapper.selectAll();

        Map<String, Integer> statusMap = new HashMap<>();
        for (DeviceStatus ds : devices) {
            statusMap.put(ds.getDeviceCode(), ds.getStatus());
        }

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        return DashboardVO.builder()
                .currentTemp(latest != null ? latest.getTemperature() : BigDecimal.ZERO)
                .currentHumidity(latest != null ? latest.getHumidity() : BigDecimal.ZERO)
                .currentSmoke(latest != null ? latest.getSmokeLevel() : BigDecimal.ZERO)
                .deviceStatus(statusMap)
                .todayAlertCount(alertLogMapper.countToday(todayStart, todayEnd))
                .todayVoiceCount(voiceLogMapper.countToday(todayStart, todayEnd))
                .build();
    }

    @Override
    public EnvChartVO getChartData(int hours) {
        List<EnvData> dataList = envDataMapper.selectRecentHours(hours);

        List<LocalDateTime> times = dataList.stream()
                .map(EnvData::getCreateTime)
                .collect(Collectors.toList());

        List<BigDecimal> temps = dataList.stream()
                .map(EnvData::getTemperature)
                .collect(Collectors.toList());

        List<BigDecimal> humids = dataList.stream()
                .map(EnvData::getHumidity)
                .collect(Collectors.toList());

        List<BigDecimal> smokes = dataList.stream()
                .map(EnvData::getSmokeLevel)
                .collect(Collectors.toList());

        // 计算统计摘要
        EnvChartVO.ChartSummary summary = null;
        if (!temps.isEmpty()) {
            BigDecimal avgTemp = temps.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(temps.size()), 2, RoundingMode.HALF_UP);
            BigDecimal maxTemp = temps.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal minTemp = temps.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal avgHumidity = humids.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(humids.size()), 2, RoundingMode.HALF_UP);
            BigDecimal maxSmoke = smokes.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            summary = EnvChartVO.ChartSummary.builder()
                    .avgTemp(avgTemp)
                    .maxTemp(maxTemp)
                    .minTemp(minTemp)
                    .avgHumidity(avgHumidity)
                    .maxSmoke(maxSmoke)
                    .build();
        }

        return EnvChartVO.builder()
                .times(times)
                .temperatures(temps)
                .humidities(humids)
                .smokeLevels(smokes)
                .summary(summary)
                .build();
    }
}
