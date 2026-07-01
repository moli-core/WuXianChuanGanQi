package com.wireless.controller;

import com.wireless.model.vo.ApiResult;
import com.wireless.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 天气信息接口
 */
@Tag(name = "天气信息", description = "本地天气预报")
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @Operation(summary = "获取当前天气")
    @GetMapping("/current")
    public ApiResult<Map<String, Object>> getCurrentWeather(@RequestParam(required = false) String city) {
        return ApiResult.success(weatherService.getCurrentWeather(city));
    }

    @Operation(summary = "获取配置的城市")
    @GetMapping("/city")
    public ApiResult<String> getCity() {
        return ApiResult.success(weatherService.getCity());
    }
}
