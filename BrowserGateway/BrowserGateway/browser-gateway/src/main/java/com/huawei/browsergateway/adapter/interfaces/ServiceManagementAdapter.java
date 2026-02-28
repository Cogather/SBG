package com.huawei.browsergateway.adapter.interfaces;

import com.huawei.browsergateway.adapter.dto.ServiceInstance;

import java.util.List;
import java.util.Map;

/**
 * 服务管理适配器接口
 * 职责：服务注册、发现、属性上报
 */
public interface ServiceManagementAdapter {
    
    /**
     * 上报服务实例属性
     * @param properties 属性键值对
     * @return 上报是否成功
     */
    boolean reportInstanceProperties(Map<String, String> properties);
    
    /**
     * 获取服务实例属性
     * @param key 属性键
     * @return 属性值
     */
    String getInstanceProperty(String key);
    
    /**
     * 查找服务实例
     * @param applicationId 应用ID
     * @param serviceName 服务名称
     * @param version 服务版本
     * @return 服务实例列表
     */
    List<ServiceInstance> findServiceInstances(String applicationId, String serviceName, String version);
    
    /**
     * 获取当前服务实例信息
     * @return 当前服务实例
     */
    ServiceInstance getCurrentInstance();
    
    /**
     * 注册REST服务
     * @param schemaId Schema ID
     * @param serviceInstance 服务实例
     * @return 注册是否成功
     */
    boolean registerRestService(String schemaId, Object serviceInstance);
}
