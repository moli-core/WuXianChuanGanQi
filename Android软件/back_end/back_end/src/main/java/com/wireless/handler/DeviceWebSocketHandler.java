package com.wireless.handler;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备状态 WebSocket 推送
 * 连接: ws://localhost:8080/ws/device-state
 */
@Slf4j
@Component
@ServerEndpoint("/ws/device-state")
public class DeviceWebSocketHandler {

    /** 所有连接的 session */
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        log.info("WebSocket 连接: {} (当前 {} 个连接)", session.getId(), sessions.size());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        log.info("WebSocket 断开: {}", session.getId());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 错误: {}", session.getId(), error);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.debug("收到客户端消息: {}", message);
    }

    /** 向所有连接的客户端广播设备状态 */
    public static void broadcast(Map<String, Object> deviceState) {
        String json = JSON.toJSONString(deviceState);
        for (Session session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(json);
                } catch (IOException e) {
                    log.error("WebSocket 推送失败", e);
                }
            }
        }
    }

    public static int getOnlineCount() {
        return sessions.size();
    }
}
