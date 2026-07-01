package com.wireless.controller;

import com.wireless.model.entity.ServerEvent;
import com.wireless.model.entity.ServerInstance;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.ServerInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "服务器管理", description = "服务器实例启停、事件日志")
@RestController
@RequestMapping("/api/server")
@RequiredArgsConstructor
public class ServerController {

    private final ServerInstanceService serverInstanceService;

    @Operation(summary = "服务器列表")
    @GetMapping
    public ApiResult<List<ServerInstance>> list() {
        return ApiResult.success(serverInstanceService.listServers());
    }

    @Operation(summary = "创建服务器实例")
    @PostMapping
    public ApiResult<?> create(@RequestBody ServerInstance server) {
        return serverInstanceService.createServer(server);
    }

    @Operation(summary = "修改服务器")
    @PutMapping("/{id}")
    public ApiResult<?> update(@PathVariable Long id, @RequestBody ServerInstance server) {
        return serverInstanceService.updateServer(id, server);
    }

    @Operation(summary = "删除服务器")
    @DeleteMapping("/{id}")
    public ApiResult<?> delete(@PathVariable Long id) {
        return serverInstanceService.deleteServer(id);
    }

    @Operation(summary = "启动服务器")
    @PostMapping("/{id}/start")
    public ApiResult<?> start(@PathVariable Long id) {
        return serverInstanceService.startServer(id);
    }

    @Operation(summary = "停止服务器")
    @PostMapping("/{id}/stop")
    public ApiResult<?> stop(@PathVariable Long id) {
        return serverInstanceService.stopServer(id);
    }

    @Operation(summary = "查看事件日志")
    @GetMapping("/events")
    public ApiResult<List<ServerEvent>> events(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String deviceCode,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResult.success(serverInstanceService.getEvents(eventType, deviceCode, limit));
    }
}
