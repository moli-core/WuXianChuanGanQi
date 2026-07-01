package com.wireless.service.impl;

import com.wireless.mapper.ServerEventMapper;
import com.wireless.mapper.ServerInstanceMapper;
import com.wireless.model.entity.ServerEvent;
import com.wireless.model.entity.ServerInstance;
import com.wireless.model.vo.ApiResult;
import com.wireless.service.ServerInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerInstanceServiceImpl implements ServerInstanceService {

    private final ServerInstanceMapper serverInstanceMapper;
    private final ServerEventMapper serverEventMapper;

    @Override
    public List<ServerInstance> listServers() {
        return serverInstanceMapper.selectAll();
    }

    @Override
    @Transactional
    public ApiResult<?> createServer(ServerInstance server) {
        serverInstanceMapper.insert(server);
        log.info("服务器实例已创建: {}", server.getServerName());
        return ApiResult.success(server);
    }

    @Override
    @Transactional
    public ApiResult<?> updateServer(Long id, ServerInstance server) {
        server.setId(id);
        serverInstanceMapper.update(server);
        return ApiResult.success();
    }

    @Override
    @Transactional
    public ApiResult<?> deleteServer(Long id) {
        serverInstanceMapper.deleteById(id);
        return ApiResult.success();
    }

    @Override
    @Transactional
    public ApiResult<?> startServer(Long id) {
        serverInstanceMapper.updateStatus(id, "running");
        recordEvent(id, "server_start", null, "服务器已启动", null);
        log.info("服务器已启动: id={}", id);
        return ApiResult.success();
    }

    @Override
    @Transactional
    public ApiResult<?> stopServer(Long id) {
        serverInstanceMapper.updateStatus(id, "stopped");
        recordEvent(id, "server_stop", null, "服务器已停止", null);
        log.info("服务器已停止: id={}", id);
        return ApiResult.success();
    }

    @Override
    public void recordEvent(Long serverId, String eventType, String deviceCode, String content, String rawData) {
        ServerEvent event = ServerEvent.builder()
                .serverId(serverId)
                .eventType(eventType)
                .deviceCode(deviceCode)
                .content(content)
                .rawData(rawData)
                .build();
        serverEventMapper.insert(event);
    }

    @Override
    public List<ServerEvent> getEvents(String eventType, String deviceCode, int limit) {
        if (deviceCode != null) {
            return serverEventMapper.selectByDevice(deviceCode, limit);
        }
        if (eventType != null) {
            return serverEventMapper.selectByType(eventType, limit);
        }
        return serverEventMapper.selectByType("device_report", limit);
    }
}
