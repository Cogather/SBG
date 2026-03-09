package com.huawei.browsergateway.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * TCP协议配置类
 * 负责管理TCP服务相关的配置信息，包括服务地址、端口、TLS端口和心跳超时时间
 */
@Data
@Configuration
public class TcpConfig {

    /** TCP服务地址，从配置文件 browsergw.tcp.address 注入 */
    @Value("${browsergw.tcp.address}")
    private String address;

    /** TCP控制通道端口，从配置文件 browsergw.tcp.control-port 注入 */
    @Value("${browsergw.tcp.control-port}")
    private Integer controlPort;

    /** TCP媒体通道端口，从配置文件 browsergw.tcp.media-port 注入 */
    @Value("${browsergw.tcp.media-port}")
    private Integer mediaPort;

    /** TCP控制通道TLS端口，从配置文件 browsergw.tcp.control-tls-port 注入 */
    @Value("${browsergw.tcp.control-tls-port}")
    private Integer controlTlsPort;

    /** TCP媒体通道TLS端口，从配置文件 browsergw.tcp.media-tls-port 注入 */
    @Value("${browsergw.tcp.media-tls-port}")
    private Integer mediaTlsPort;

    /** 心跳超时时间（毫秒），从配置文件 browsergw.tcp.heartbeat-ttl 注入 */
    @Value("${browsergw.tcp.heartbeat-ttl}")
    private long heartbeatTtl;

    /** HTTP功能开关，从配置文件 browsergw.tcp.enable-http 注入 */
    @Value("${browsergw.tcp.enable-http}")
    private boolean enableHttp;
}
