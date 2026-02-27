package com.huawei.browsergateway.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 服务上报配置类
 * 对应存量代码中的ReportConfig类
 */
@Data
@Configuration
public class ReportConfig {

    @Value("${browsergw.report.control-endpoint}")
    private String controlEndpoint;

    @Value("#{'${browsergw.report.control-tls-endpoint}'.empty ? '41.203.73.4:30011' : '${browsergw.report.control-tls-endpoint}'}")
    private String controlTlsEndpoint;

    @Value("${browsergw.report.media-endpoint}")
    private String mediaEndpoint;

    @Value("#{'${browsergw.report.media-tls-endpoint}'.empty ? '41.203.73.4:30013' : '${browsergw.report.media-tls-endpoint}'}")
    private String mediaTlsEndpoint;

    @Value("${browsergw.report.cap}")
    private Integer cap;

    @Value("${browsergw.report.ttl}")
    private Integer ttl;

    @Value("${browsergw.report.chain-endpoints:}")
    private String chainEndpoints;

    @Value("${browsergw.report.self-addr:}")
    private String selfAddr;
}
