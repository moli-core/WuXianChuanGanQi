package com.wireless.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源配置 — 映射数字孪生页面到 /digital-twin
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /digital-twin/** 映射到 web3d1 目录
        registry.addResourceHandler("/digital-twin/**")
                .addResourceLocations("file:///D:/wireless/web3d1/web3d/web3d/");
    }
}
