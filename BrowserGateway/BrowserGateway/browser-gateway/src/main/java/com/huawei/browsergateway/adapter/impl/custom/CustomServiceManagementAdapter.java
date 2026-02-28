package com.huawei.browsergateway.adapter.impl.custom;

import com.huawei.browsergateway.adapter.dto.ServiceInstance;
import com.huawei.browsergateway.adapter.interfaces.ServiceManagementAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务管理适配器 - 自定义实现
 */
@Component("customServiceManagementAdapter")
@ConditionalOnProperty(name = "adapter.provider.type", havingValue = "CUSTOM")
public class CustomServiceManagementAdapter implements ServiceManagementAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomServiceManagementAdapter.class);
    
    private final Map<String, String> instanceProperties = new ConcurrentHashMap<>();
    private final Map<String, List<ServiceInstance>> serviceInstances = new ConcurrentHashMap<>();
    private ServiceInstance currentInstance;
    
    @Value("${adapter.custom.service-name:browser-gateway}")
    private String serviceName;
    
    @Value("${adapter.custom.pod-name:browser-gateway-pod-1}")
    private String podName;
    
    @Value("${server.port:8090}")
    private int serverPort;
    
    public CustomServiceManagementAdapter() {
        // 初始化当前实例
        currentInstance = new ServiceInstance();
        currentInstance.setInstanceId("custom-instance-1");
        currentInstance.setServiceName(serviceName);
        currentInstance.setHost("127.0.0.1");
        currentInstance.setPort(serverPort);
        currentInstance.setProtocol("http");
        currentInstance.setStatus(ServiceInstance.InstanceStatus.UP);
        currentInstance.setProperties(new HashMap<>());
        currentInstance.setEndpoints(new ArrayList<>());
        currentInstance.getEndpoints().add("http://127.0.0.1:" + serverPort);
    }
    
    @Override
    public boolean reportInstanceProperties(Map<String, String> properties) {
        if (properties != null) {
            instanceProperties.putAll(properties);
            logger.info("Reported instance properties: {}", properties);
        }
        return true;
    }
    
    @Override
    public String getInstanceProperty(String key) {
        return instanceProperties.get(key);
    }
    
    @Override
    public List<ServiceInstance> findServiceInstances(String applicationId, String serviceName, String version) {
        // 返回模拟的服务实例列表
        List<ServiceInstance> instances = serviceInstances.get(serviceName);
        if (instances == null) {
            instances = new ArrayList<>();
            // 创建模拟实例
            ServiceInstance instance = new ServiceInstance();
            instance.setInstanceId("mock-instance-" + serviceName);
            instance.setServiceName(serviceName);
            instance.setHost("127.0.0.1");
            instance.setPort(8090);
            instance.setProtocol("http");
            instance.setStatus(ServiceInstance.InstanceStatus.UP);
            instance.setProperties(new HashMap<>());
            instance.setEndpoints(new ArrayList<>());
            instance.getEndpoints().add("http://127.0.0.1:8090");
            instances.add(instance);
            serviceInstances.put(serviceName, instances);
        }
        return new ArrayList<>(instances);
    }
    
    @Override
    public ServiceInstance getCurrentInstance() {
        return currentInstance;
    }
    
    @Override
    public boolean registerRestService(String schemaId, Object serviceInstance) {
        logger.info("REST service registered: {}", schemaId);
        return true;
    }
}
