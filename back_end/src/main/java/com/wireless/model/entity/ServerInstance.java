package com.wireless.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 服务器实例实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerInstance {
    private Long id;
    private String serverName;     // 服务器名称
    private Integer listenPort;    // 监听端口
    private String protocol;       // mqtt/http/tcp
    private String serverType;     // 服务器类型
    private String description;    // 描述
    private String status;         // running/stopped/error
    private Integer pid;           // 进程 PID
    private String logPath;
    private LocalDateTime lastStart;
    private LocalDateTime lastStop;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
