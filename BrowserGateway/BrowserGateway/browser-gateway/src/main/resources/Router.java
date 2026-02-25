package com.huawei.browsergateway.router;

import com.huawei.browsergateway.adapter.dto.ServiceInstance;
import com.huawei.browsergateway.adapter.interfaces.ServiceManagementAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 路由模块
 * 职责：请求路由分发、负载均衡、服务发现
 */
@Component
public class Router {
    
    @Autowired
    private ServiceManagementAdapter serviceManagementAdapter;
    
    /**
     * 路由到指定服务
     */
    public String route(String serviceName, String operation) {
        List<ServiceInstance> instances = serviceManagementAdapter.findServiceInstances(serviceName);
        if (instances == null || instances.isEmpty()) {
            throw new RuntimeException("No available instances for service: " + serviceName);
        }
        return selectInstance(instances, operation);
    }
    
    /**
     * 选择服务实例（简单轮询）
     */
    private String selectInstance(List<ServiceInstance> instances, String operation) {
        int index = (int) (System.currentTimeMillis() % instances.size());
        ServiceInstance instance = instances.get(index);
        return instance.getHost() + ":" + instance.getPort();
    }
}
