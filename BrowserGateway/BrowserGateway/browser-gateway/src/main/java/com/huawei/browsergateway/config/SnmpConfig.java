package com.huawei.browsergateway.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * SNMP客户端配置类
 * SNMP服务端地址为固定值192.168.16.4，由VNFD注入环境变量SNMP_SERVER_IP
 */
@Data
@Configuration
public class SnmpConfig {

    @Value("${snmp.server.ip:192.168.16.4}")
    private String serverIp;

    @Value("${snmp.server.port:162}")
    private int serverPort;

    @Value("${snmp.timeout:10000}")
    private int timeout;

    @Value("${snmp.retry.times:3}")
    private int retryTimes;

    @Value("${snmp.retry.interval:5000}")
    private int retryInterval;

    public String[] getServerIPs() {
        return new String[]{serverIp};
    }
}