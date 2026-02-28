package com.huawei.browsergateway.adapter.dto;

import java.util.List;
import java.util.Map;

/**
 * 服务实例
 */
public class ServiceInstance {
    private String instanceId;
    private String serviceName;
    private String host;
    private int port;
    private String protocol;
    private InstanceStatus status;
    private List<String> endpoints;
    private Map<String, String> properties;
    
    public enum InstanceStatus {
        UP, DOWN, STARTING, OUT_OF_SERVICE
    }
    
    public String getInstanceId() {
        return instanceId;
    }
    
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    public InstanceStatus getStatus() {
        return status;
    }
    
    public void setStatus(InstanceStatus status) {
        this.status = status;
    }
    
    public List<String> getEndpoints() {
        return endpoints;
    }
    
    public void setEndpoints(List<String> endpoints) {
        this.endpoints = endpoints;
    }
    
    public Map<String, String> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
