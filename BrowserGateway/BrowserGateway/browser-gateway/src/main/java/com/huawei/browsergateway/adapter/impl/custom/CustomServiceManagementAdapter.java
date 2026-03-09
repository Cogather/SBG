package com.huawei.browsergateway.adapter.impl.custom;

import com.huawei.browsergateway.adapter.dto.ServiceInstance;
import com.huawei.browsergateway.adapter.ServiceManagementAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务管理适配器 - 自定义实现
 */
public class CustomServiceManagementAdapter implements ServiceManagementAdapter {
    
    private static final Logger logger = LogManager.getLogger(CustomServiceManagementAdapter.class);
    
    private final Map<String, String> instanceProperties = new ConcurrentHashMap<>();
    private final Map<String, List<ServiceInstance>> serviceInstances = new ConcurrentHashMap<>();
    private ServiceInstance currentInstance;
    
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
            logger.info("No instances found for the service name {}", serviceName);
            return new ArrayList<>();
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
