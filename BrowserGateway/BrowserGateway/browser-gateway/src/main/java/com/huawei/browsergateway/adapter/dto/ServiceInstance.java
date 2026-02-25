package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

import java.util.Map;

/**
 * 服务实例
 */
@Data
public class ServiceInstance {
    private String instanceId;
    private String serviceName;
    private String host;
    private int port;
    private String protocol;
    private InstanceStatus status;
    private Map<String, String> properties;
    
    public enum InstanceStatus {
        UP, DOWN, STARTING, OUT_OF_SERVICE
    }
}
