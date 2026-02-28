package com.huawei.browsergateway.adapter.impl.csp;

import com.huawei.browsergateway.adapter.dto.ServiceInstance;
import com.huawei.browsergateway.adapter.interfaces.ServiceManagementAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 服务管理适配器 - CSP SDK实现
 */
@Component("cspServiceManagementAdapter")
@ConditionalOnProperty(name = "adapter.provider.type", havingValue = "CSP_SDK", matchIfMissing = true)
public class CspServiceManagementAdapter implements ServiceManagementAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CspServiceManagementAdapter.class);
    
    @Override
    public boolean reportInstanceProperties(Map<String, String> properties) {
        try {
            Class<?> serviceUtilsClass = Class.forName("com.huawei.csp.csejsdk.common.utils.ServiceUtils");
            Method putInstanceProperties = serviceUtilsClass.getMethod("putInstanceProperties", Map.class);
            Boolean result = (Boolean) putInstanceProperties.invoke(null, properties);
            return result != null && result;
        } catch (Exception e) {
            logger.error("Failed to report instance properties via CSP SDK", e);
            return false;
        }
    }
    
    @Override
    public String getInstanceProperty(String key) {
        try {
            Class<?> serviceUtilsClass = Class.forName("com.huawei.csp.csejsdk.common.utils.ServiceUtils");
            Method getInstanceProperty = serviceUtilsClass.getMethod("getInstanceProperty", String.class);
            return (String) getInstanceProperty.invoke(null, key);
        } catch (Exception e) {
            logger.error("Failed to get instance property via CSP SDK", e);
            return null;
        }
    }
    
    @Override
    public List<ServiceInstance> findServiceInstances(String applicationId, String serviceName, String version) {
        try {
            Class<?> registryUtilsClass = Class.forName("org.apache.servicecomb.serviceregistry.RegistryUtils");
            Method findServiceInstance = registryUtilsClass.getMethod("findServiceInstance", 
                    String.class, String.class, String.class);
            @SuppressWarnings("unchecked")
            List<Object> instances = (List<Object>) findServiceInstance.invoke(null, applicationId, serviceName, version);
            
            if (instances == null) {
                return new ArrayList<>();
            }
            
            List<ServiceInstance> result = new ArrayList<>();
            for (Object instance : instances) {
                ServiceInstance si = convertToServiceInstance(instance);
                if (si != null) {
                    result.add(si);
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Failed to find service instances via CSP SDK", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public ServiceInstance getCurrentInstance() {
        try {
            Class<?> registryUtilsClass = Class.forName("org.apache.servicecomb.serviceregistry.RegistryUtils");
            Method getMicroserviceInstance = registryUtilsClass.getMethod("getMicroserviceInstance");
            Object instance = getMicroserviceInstance.invoke(null);
            
            return convertToServiceInstance(instance);
        } catch (Exception e) {
            logger.error("Failed to get current instance via CSP SDK", e);
            return null;
        }
    }
    
    @Override
    public boolean registerRestService(String schemaId, Object serviceInstance) {
        // REST服务通过@RestSchema注解自动注册，这里返回true表示支持
        logger.info("REST service registration is handled by @RestSchema annotation");
        return true;
    }
    
    private ServiceInstance convertToServiceInstance(Object instance) {
        if (instance == null) {
            return null;
        }
        
        try {
            ServiceInstance si = new ServiceInstance();
            
            // 使用反射获取字段值
            Method getInstanceId = instance.getClass().getMethod("getInstanceId");
            Method getServiceName = instance.getClass().getMethod("getServiceName");
            Method getEndpoints = instance.getClass().getMethod("getEndpoints");
            Method getStatus = instance.getClass().getMethod("getStatus");
            Method getProperties = instance.getClass().getMethod("getProperties");
            
            si.setInstanceId((String) getInstanceId.invoke(instance));
            si.setServiceName((String) getServiceName.invoke(instance));
            @SuppressWarnings("unchecked")
            List<String> endpoints = (List<String>) getEndpoints.invoke(instance);
            si.setEndpoints(endpoints);
            
            // 转换状态
            Object status = getStatus.invoke(instance);
            if (status != null) {
                String statusStr = status.toString();
                try {
                    si.setStatus(ServiceInstance.InstanceStatus.valueOf(statusStr));
                } catch (IllegalArgumentException e) {
                    si.setStatus(ServiceInstance.InstanceStatus.UP);
                }
            }
            
            @SuppressWarnings("unchecked")
            Map<String, String> properties = (Map<String, String>) getProperties.invoke(instance);
            si.setProperties(properties);
            
            // 从endpoints解析host和port
            if (endpoints != null && !endpoints.isEmpty()) {
                String endpoint = endpoints.get(0);
                if (endpoint != null && endpoint.startsWith("http")) {
                    try {
                        java.net.URI uri = new java.net.URI(endpoint);
                        si.setHost(uri.getHost());
                        si.setPort(uri.getPort());
                        si.setProtocol(uri.getScheme());
                    } catch (Exception e) {
                        logger.warn("Failed to parse endpoint: {}", endpoint, e);
                    }
                }
            }
            
            return si;
        } catch (Exception e) {
            logger.error("Failed to convert instance to ServiceInstance", e);
            return null;
        }
    }
}
