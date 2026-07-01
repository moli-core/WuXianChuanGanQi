package com.wireless.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class IndexController {

    @GetMapping("/")
    public Map<String, Object> index() {
        return Map.of(
                "service", "AIOT 无线传感网后端",
                "version", "2.0",
                "status", "running",
                "swagger", "/swagger-ui.html",
                "websocket", "ws://HOST:8080/ws/device-state",
                "docs", "/v3/api-docs"
        );
    }
}
