package com.huawei.browsergateway.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket参数配置类
 * 对应存量代码中的WebsocketConfig类
 * 注意：与WebSocketConfig区分，该类用于参数配置
 */
@Data
@Configuration
public class WebsocketConfig {

    @Value("${browsergw.websocket.media-port}")
    private Integer mediaPort;

    @Value("${browsergw.websocket.muen-port}")
    private Integer muenPort;

    @Value("${browsergw.websocket.boss}")
    private Integer boss;

    @Value("${browsergw.websocket.worker}")
    private Integer worker;

    @Value("${browsergw.websocket.heartbeat-ttl}")
    private Long heartbeatTtl;
}
