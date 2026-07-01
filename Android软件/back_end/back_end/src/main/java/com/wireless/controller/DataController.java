package com.wireless.controller;

import com.wireless.mapper.AiotDeviceMapper;
import com.wireless.mapper.EnvDataMapper;
import com.wireless.model.dto.DataQueryRequest;
import com.wireless.model.entity.AiotDevice;
import com.wireless.model.entity.EnvData;
import com.wireless.model.vo.ApiResult;
import com.wireless.model.vo.DashboardVO;
import com.wireless.model.vo.EnvChartVO;
import com.wireless.service.EnvDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 环境数据接口
 */
@Tag(name = "环境数据", description = "实时数据、历史数据、图表")
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataController {

    private final EnvDataService envDataService;
    private final EnvDataMapper envDataMapper;
    private final AiotDeviceMapper aiotDeviceMapper;

    @Operation(summary = "ESP32 上报环境数据 (华为 IoT 转发或直连)")
    @PostMapping("/report")
    public ApiResult<?> receiveReport(@RequestBody Map<String, Object> body) {
        // 解析华为 IoT 转发的标准格式: {"services":[{...}]} 或 ESP32 直传 JSON
        Map<String, Object> props = body;
        if (body.containsKey("services")) {
            var services = (java.util.List<Map<String, Object>>) body.get("services");
            if (!services.isEmpty()) props = services.get(0);
            if (props.containsKey("properties")) {
                props = (Map<String, Object>) props.get("properties");
            }
        }

        // 保存环境数据
        EnvData env = EnvData.builder()
                .deviceCode((String) props.getOrDefault("device_code", "esp32_001"))
                .temperature(toDecimal(props.get("Data_temp")))
                .humidity(toDecimal(props.get("Data_humi")))
                .smokeLevel(toDecimal(props.get("smoke")))
                .build();
        envDataMapper.insert(env);

        // 同步更新设备状态
        String deviceCode = (String) props.getOrDefault("device_code", "esp32_001");
        AiotDevice device = AiotDevice.builder()
                .deviceCode(deviceCode)
                .ledStatus(toInt(props.get("Status_LED")))
                .pirStatus(toInt(props.get("Status_body")))
                .onlineStatus(1)
                .build();
        aiotDeviceMapper.upsertOnReport(device);

        return ApiResult.success();
    }

    private BigDecimal toDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal bd) return bd;
        try { return new BigDecimal(val.toString()); }
        catch (Exception e) { return null; }
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Integer i) return i;
        if (val instanceof Boolean b) return b ? 1 : 0;
        if (val instanceof String s) {
            if ("true".equalsIgnoreCase(s)) return 1;
            if ("false".equalsIgnoreCase(s)) return 0;
        }
        try { return Integer.parseInt(val.toString()); }
        catch (Exception e) { return null; }
    }

    @Operation(summary = "获取仪表盘数据 (实时总览)")
    @GetMapping("/dashboard")
    public ApiResult<DashboardVO> getDashboard() {
        return ApiResult.success(envDataService.getDashboard());
    }

    @Operation(summary = "获取当前最新环境数据")
    @GetMapping("/latest")
    public ApiResult<?> getLatest() {
        return ApiResult.success(envDataService.getLatest());
    }

    @Operation(summary = "获取图表数据 (近N小时)")
    @GetMapping("/chart")
    public ApiResult<EnvChartVO> getChart(@RequestParam(defaultValue = "24") int hours) {
        return ApiResult.success(envDataService.getChartData(hours));
    }

    @Operation(summary = "查询历史数据 (时间范围)")
    @PostMapping("/history")
    public ApiResult<?> getHistory(@RequestBody DataQueryRequest request) {
        return ApiResult.success();
    }
}
