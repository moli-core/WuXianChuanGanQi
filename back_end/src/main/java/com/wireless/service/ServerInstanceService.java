package com.wireless.service;

import com.wireless.model.entity.ServerInstance;
import com.wireless.model.entity.ServerEvent;
import com.wireless.model.vo.ApiResult;

import java.util.List;

public interface ServerInstanceService {
    List<ServerInstance> listServers();
    ApiResult<?> createServer(ServerInstance server);
    ApiResult<?> updateServer(Long id, ServerInstance server);
    ApiResult<?> deleteServer(Long id);
    ApiResult<?> startServer(Long id);
    ApiResult<?> stopServer(Long id);

    /** 记录服务器事件 */
    void recordEvent(Long serverId, String eventType, String deviceCode, String content, String rawData);
    List<ServerEvent> getEvents(String eventType, String deviceCode, int limit);
}
