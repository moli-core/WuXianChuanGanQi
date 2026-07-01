package com.wireless;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 无线智能家居后端启动类
 */
@SpringBootApplication
@EnableScheduling
public class WirelessApplication {

    public static void main(String[] args) {
        SpringApplication.run(WirelessApplication.class, args);
    }
}
