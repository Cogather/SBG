package com.huawei.browsergateway.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 服务上报配置类
 * 负责管理服务上报相关的配置信息，包括控制通道、媒体通道端点和上报参数
 */
@Data
@Configuration
public class ReportConfig {

    /** 控制通道端点地址，从配置文件 browsergw.report.control-endpoint 注入 */
    @Value("${browsergw.report.control-endpoint}")
    private String controlEndpoint;

    /** 控制通道TLS端点地址，从配置文件 browsergw.report.control-tls-endpoint 注入，默认值为 41.203.73.4:30011 */
    @Value("#{'${browsergw.report.control-tls-endpoint}'.empty ? '41.203.73.4:30011' : '${browsergw.report.control-tls-endpoint}'}")
    private String controlTlsEndpoint;

    /** 媒体通道端点地址，从配置文件 browsergw.report.media-endpoint 注入 */
    @Value("${browsergw.report.media-endpoint}")
    private String mediaEndpoint;

    /** 媒体通道TLS端点地址，从配置文件 browsergw.report.media-tls-endpoint 注入，默认值为 41.203.73.4:30013 */
    @Value("#{'${browsergw.report.media-tls-endpoint}'.empty ? '41.203.73.4:30013' : '${browsergw.report.media-tls-endpoint}'}")
    private String mediaTlsEndpoint;

    /** 上报容量限制，从配置文件 browsergw.report.cap 注入 */
    @Value("${browsergw.report.cap}")
    private Integer cap;

    /** 上报数据生存时间，从配置文件 browsergw.report.ttl 注入 */
    @Value("${browsergw.report.ttl}")
    private Integer ttl;

    /** 链式端点地址列表，从配置文件 browsergw.report.chain-endpoints 注入 */
    @Value("${browsergw.report.chain-endpoints}")
    private String chainEndpoints;

    /** 服务自身地址，从配置文件 browsergw.report.self-addr 注入 */
    @Value("${browsergw.report.self-addr}")
    private String selfAddr;
}
