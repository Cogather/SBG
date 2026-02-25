package com.huawei.browsergateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类
 * 用于注册@ServerEndpoint注解的WebSocket端点
 */
@Configuration
public class WebSocketConfig {
    
    /**
     * 注册WebSocket端点
     * 如果使用内嵌的Tomcat，需要这个Bean
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
