package com.huawei.browsergateway.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket参数配置类
 * 负责管理WebSocket服务相关的配置信息，包括服务端口、Netty线程池和心跳超时时间
 */
@Data
@Configuration
public class WebsocketConfig {

    /** WebSocket媒体端口，从配置文件 browsergw.websocket.media-port 注入 */
    @Value("${browsergw.websocket.media-port}")
    private Integer mediaPort;

    /** WebSocket muen端口，从配置文件 browsergw.websocket.muen-port 注入 */
    @Value("${browsergw.websocket.muen-port}")
    private Integer muenPort;

    /** Netty boss线程数，从配置文件 browsergw.websocket.boss 注入 */
    @Value("${browsergw.websocket.boss}")
    private Integer boss;

    /** Netty worker线程数，从配置文件 browsergw.websocket.worker 注入 */
    @Value("${browsergw.websocket.worker}")
    private Integer worker;

    /** 心跳超时时间（毫秒），从配置文件 browsergw.websocket.heartbeat-ttl 注入 */
    @Value("${browsergw.websocket.heartbeat-ttl}")
    private Long heartbeatTtl;
}
