package com.huawei.browsergateway.adapter.impl.csp;

import com.huawei.browsergateway.adapter.dto.ServiceInstance;
import com.huawei.browsergateway.adapter.interfaces.ServiceManagementAdapter;
import com.huawei.csp.csejsdk.common.utils.ServiceUtils;
import org.apache.servicecomb.registry.api.registry.MicroserviceInstance;
import org.apache.servicecomb.registry.api.registry.MicroserviceInstanceStatus;
import org.apache.servicecomb.serviceregistry.RegistryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 服务管理适配器 - CSP SDK实现
 * 适用场景：内网环境，使用ServiceComb的服务注册发现
 */
@Component
public class CspServiceManagementAdapter implements ServiceManagementAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CspServiceManagementAdapter.class);
    
    @Override
    public boolean reportInstanceProperties(Map<String, String> properties) {
        try {
            // 使用CSP SDK的ServiceUtils上报属性
            boolean success = ServiceUtils.putInstanceProperties(properties);
            if (success) {
                logger.info("Instance properties reported successfully: {}", properties.keySet());
            } else {
                logger.warn("Failed to report instance properties");
            }
            return success;
        } catch (Exception e) {
            logger.error("Failed to report instance properties", e);
            return false;
        }
    }
    
    @Override
    public String getInstanceProperty(String key) {
        try {
            // 使用CSP SDK获取实例属性
            return ServiceUtils.getInstanceProperty(key);
        } catch (Exception e) {
            logger.error("Failed to get instance property: {}", key, e);
            return null;
        }
    }
    
    @Override
    public List<ServiceInstance> findServiceInstances(String serviceName) {
        try {
            // 使用CSP SDK的RegistryUtils查找服务实例
            List<MicroserviceInstance> instances = RegistryUtils.findServiceInstance("0", serviceName, "0+");
            
            List<ServiceInstance> serviceInstances = new ArrayList<>();
            if (instances != null) {
                for (MicroserviceInstance instance : instances) {
                    // 只返回状态为UP的实例
                    if (instance.getStatus() != MicroserviceInstanceStatus.UP) {
                        continue;
                    }
                    
                    ServiceInstance serviceInstance = convertToServiceInstance(instance);
                    if (serviceInstance != null) {
                        serviceInstances.add(serviceInstance);
                    }
                }
            }
            
            logger.info("Found {} service instances for service: {}", serviceInstances.size(), serviceName);
            return serviceInstances;
        } catch (Exception e) {
            logger.error("Failed to find service instances: {}", serviceName, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public ServiceInstance getCurrentInstance() {
        try {
            // 使用CSP SDK获取当前实例
            MicroserviceInstance instance = RegistryUtils.getMicroserviceInstance();
            if (instance != null) {
                return convertToServiceInstance(instance);
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to get current instance", e);
            return null;
        }
    }
    
    @Override
    public boolean registerRestService(String schemaId, Object serviceInstance) {
        try {
            // REST服务通过@RestSchema注解自动注册，这里仅记录日志
            // @RestSchema注解会在Spring启动时自动处理服务注册
            logger.info("REST service registered: {}", schemaId);
            return true;
        } catch (Exception e) {
            logger.error("Failed to register REST service: {}", schemaId, e);
            return false;
        }
    }
    
    /**
     * 将CSP SDK的MicroserviceInstance转换为ServiceInstance
     */
    private ServiceInstance convertToServiceInstance(MicroserviceInstance instance) {
        if (instance == null) {
            return null;
        }
        
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setInstanceId(instance.getInstanceId());
        serviceInstance.setServiceName(instance.getServiceName());
        
        // 转换状态
        if (instance.getStatus() == MicroserviceInstanceStatus.UP) {
            serviceInstance.setStatus(ServiceInstance.InstanceStatus.UP);
        } else if (instance.getStatus() == MicroserviceInstanceStatus.DOWN) {
            serviceInstance.setStatus(ServiceInstance.InstanceStatus.DOWN);
        } else if (instance.getStatus() == MicroserviceInstanceStatus.STARTING) {
            serviceInstance.setStatus(ServiceInstance.InstanceStatus.STARTING);
        } else {
            serviceInstance.setStatus(ServiceInstance.InstanceStatus.OUT_OF_SERVICE);
        }
        
        // 解析端点信息
        List<String> endpoints = instance.getEndpoints();
        if (endpoints != null && !endpoints.isEmpty()) {
            try {
                String endpoint = endpoints.get(0);
                URI uri = new URI(endpoint);
                serviceInstance.setHost(uri.getHost());
                serviceInstance.setPort(uri.getPort());
                serviceInstance.setProtocol(uri.getScheme());
            } catch (URISyntaxException e) {
                logger.warn("Failed to parse endpoint: {}", endpoints.get(0), e);
            }
        }
        
        // 设置属性
        serviceInstance.setProperties(instance.getProperties());
        
        return serviceInstance;
    }
}
