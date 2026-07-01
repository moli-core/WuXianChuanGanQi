package com.wireless.controller;

import com.wireless.model.dto.MapRouteRequest;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "地图服务", description = "路线规划、地点搜索")
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @Operation(summary = "路线规划")
    @PostMapping("/route")
    public ApiResult<Map<String, Object>> route(@RequestBody MapRouteRequest request) {
        String origin = request.getOrigin();
        if (request.getOriginCity() != null) origin = request.getOriginCity() + request.getOrigin();
        String dest = request.getDestination();
        if (request.getDestinationCity() != null) dest = request.getDestinationCity() + request.getDestination();
        return ApiResult.success(mapService.getRoute(origin, dest));
    }

    @Operation(summary = "地点搜索")
    @GetMapping("/search")
    public ApiResult<Map<String, Object>> search(@RequestParam String keyword) {
        return ApiResult.success(mapService.searchPlace(keyword));
    }
}
