package com.huawei.browsergateway.adapter.impl.custom;

import com.huawei.browsergateway.adapter.dto.ServiceInstance;
import com.huawei.browsergateway.adapter.interfaces.ServiceManagementAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务管理适配器 - 自定义实现
 * 适用场景：外网环境，使用本地内存服务注册表
 */
@Component
public class CustomServiceManagementAdapter implements ServiceManagementAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomServiceManagementAdapter.class);
    
    private final Map<String, String> instanceProperties = new ConcurrentHashMap<>();
    private final Map<String, List<ServiceInstance>> serviceRegistry = new ConcurrentHashMap<>();
    private ServiceInstance currentInstance;
    
    @Override
    public boolean reportInstanceProperties(Map<String, String> properties) {
        try {
            instanceProperties.putAll(properties);
            logger.info("Instance properties reported successfully (custom implementation)");
            return true;
        } catch (Exception e) {
            logger.error("Failed to report instance properties", e);
            return false;
        }
    }
    
    @Override
    public String getInstanceProperty(String key) {
        return instanceProperties.get(key);
    }
    
    @Override
    public List<ServiceInstance> findServiceInstances(String serviceName) {
        return serviceRegistry.getOrDefault(serviceName, new ArrayList<>());
    }
    
    @Override
    public ServiceInstance getCurrentInstance() {
        return currentInstance;
    }
    
    @Override
    public boolean registerRestService(String schemaId, Object serviceInstance) {
        try {
            logger.info("REST service registered: {} (custom implementation)", schemaId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to register REST service: {}", schemaId, e);
            return false;
        }
    }
}
