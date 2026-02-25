package com.huawei.browsergateway.adapter.factory;

import com.huawei.browsergateway.adapter.interfaces.*;

/**
 * 适配器工厂接口
 * 职责：根据环境创建合适的适配器实例
 */
public interface AdapterFactory {
    
    /**
     * 创建框架适配器
     * @return 框架适配器实例
     */
    FrameworkAdapter createFrameworkAdapter();
    
    /**
     * 创建告警适配器
     * @return 告警适配器实例
     */
    AlarmAdapter createAlarmAdapter();
    
    /**
     * 创建证书适配器
     * @return 证书适配器实例
     */
    CertificateAdapter createCertificateAdapter();
    
    /**
     * 创建服务管理适配器
     * @return 服务管理适配器实例
     */
    ServiceManagementAdapter createServiceManagementAdapter();
    
    /**
     * 创建系统工具适配器
     * @return 系统工具适配器实例
     */
    SystemUtilAdapter createSystemUtilAdapter();
    
    /**
     * 创建资源监控适配器
     * @return 资源监控适配器实例
     */
    ResourceMonitorAdapter createResourceMonitorAdapter();
}
