package com.huawei.browsergateway.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * TCP协议配置类
 * 对应存量代码中的TcpConfig类
 */
@Data
@Configuration
public class TcpConfig {

    @Value("${browsergw.tcp.address}")
    private String address;

    @Value("${browsergw.tcp.control-port}")
    private Integer controlPort;

    @Value("${browsergw.tcp.media-port}")
    private Integer mediaPort;

    @Value("${browsergw.tcp.control-tls-port}")
    private Integer controlTlsPort;

    @Value("${browsergw.tcp.media-tls-port}")
    private Integer mediaTlsPort;

    @Value("${browsergw.tcp.heartbeat-ttl}")
    private long heartbeatTtl;

    @Value("${browsergw.tcp.enable-http}")
    private boolean enableHttp;
}
