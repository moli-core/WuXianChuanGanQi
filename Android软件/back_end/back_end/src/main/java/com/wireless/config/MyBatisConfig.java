package com.wireless.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置
 */
@Configuration
@MapperScan("com.wireless.mapper")
public class MyBatisConfig {
}
