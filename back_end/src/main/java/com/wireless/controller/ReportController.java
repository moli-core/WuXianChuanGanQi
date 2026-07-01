package com.wireless.controller;

import com.wireless.model.vo.ApiResult;
import com.wireless.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "数据报表", description = "ECharts 统计数据 (折线图/环图/柱状图)")
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "设备总览统计")
    @GetMapping("/device-stats")
    public ApiResult<Map<String, Object>> deviceStats() {
        return ApiResult.success(reportService.getDeviceStats());
    }

    @Operation(summary = "事件趋势 (折线图)")
    @GetMapping("/event-trends")
    public ApiResult<Map<String, Object>> eventTrends(@RequestParam(defaultValue = "30") int days) {
        return ApiResult.success(reportService.getEventTrends(days));
    }

    @Operation(summary = "事件类型分布 (环图)")
    @GetMapping("/event-distribution")
    public ApiResult<Map<String, Object>> eventDistribution() {
        return ApiResult.success(reportService.getEventDistribution());
    }

    @Operation(summary = "设备活跃排行 (柱状图)")
    @GetMapping("/device-ranking")
    public ApiResult<Map<String, Object>> deviceRanking(@RequestParam(defaultValue = "10") int limit) {
        return ApiResult.success(reportService.getDeviceActivityRanking(limit));
    }

    @Operation(summary = "消息历史")
    @GetMapping("/message-history")
    public ApiResult<Map<String, Object>> messageHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResult.success(reportService.getMessageHistory(page, pageSize));
    }
}
