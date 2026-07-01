package com.wireless.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 服务器事件日志实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerEvent {
    private Long id;
    private Long serverId;
    private String eventType;      // device_online/offline/report/id_report/disconnect/server_start/stop/error
    private String deviceCode;
    private String content;
    private String rawData;
    private LocalDateTime createTime;
}
