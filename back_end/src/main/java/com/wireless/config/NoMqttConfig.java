package com.wireless.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

/**
 * MQTT 未配置时的兜底通道
 * 仅当 mqtt.client-id 为空时生效，提供空的 MessageChannel 让 Spring 容器正常启动
 */
@Configuration
@ConditionalOnExpression("'${mqtt.client-id:}'.isEmpty()")
public class NoMqttConfig {

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }
}
