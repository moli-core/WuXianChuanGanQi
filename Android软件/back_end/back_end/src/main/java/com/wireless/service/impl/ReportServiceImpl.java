package com.wireless.service.impl;

import com.wireless.mapper.*;
import com.wireless.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final AiotDeviceMapper aiotDeviceMapper;
    private final ServerEventMapper serverEventMapper;
    private final ServerInstanceMapper serverInstanceMapper;
    private final VoiceLogMapper voiceLogMapper;
    private final AlertLogMapper alertLogMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    public Map<String, Object> getDeviceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDevices", aiotDeviceMapper.countAll());
        stats.put("onlineDevices", aiotDeviceMapper.countOnline());
        stats.put("offlineDevices", aiotDeviceMapper.countAll() - aiotDeviceMapper.countOnline());
        stats.put("totalServers", serverInstanceMapper.selectAll().size());
        // 今日数据
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrow = today.plusDays(1);
        stats.put("todayVoiceCount", voiceLogMapper.countToday(today, tomorrow));
        stats.put("todayAlertCount", alertLogMapper.countToday(today, tomorrow));
        stats.put("todayChatCount", chatMessageMapper.countToday(today, tomorrow));
        return stats;
    }

    @Override
    public Map<String, Object> getEventTrends(int days) {
        Map<String, Object> result = new HashMap<>();
        result.put("days", days);
        result.put("data", serverEventMapper.countTrendByDay(days));
        return result;
    }

    @Override
    public Map<String, Object> getEventDistribution() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> result = new HashMap<>();
        result.put("data", serverEventMapper.countByType(now.minusDays(30), now));
        return result;
    }

    @Override
    public Map<String, Object> getDeviceActivityRanking(int limit) {
        Map<String, Object> result = new HashMap<>();
        result.put("limit", limit);
        result.put("data", serverEventMapper.deviceActivityRanking(limit));
        return result;
    }

    @Override
    public Map<String, Object> getMessageHistory(int page, int pageSize) {
        // 聚合多种消息历史
        Map<String, Object> result = new HashMap<>();
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("voiceLogs", voiceLogMapper.selectAll());
        result.put("serverEvents", serverEventMapper.selectByType(null, pageSize));
        result.put("alerts", alertLogMapper.selectByTimeRange(
                LocalDateTime.now().minusDays(7), LocalDateTime.now()));
        return result;
    }
}
