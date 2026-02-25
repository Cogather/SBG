package com.huawei.browsergateway.adapter.factory;

import com.huawei.browsergateway.adapter.impl.custom.*;
import com.huawei.browsergateway.adapter.interfaces.*;
import org.springframework.stereotype.Component;

/**
 * 自定义适配器工厂
 * 创建自定义实现，用于外网环境
 */
@Component
public class CustomAdapterFactory implements AdapterFactory {
    
    @Override
    public FrameworkAdapter createFrameworkAdapter() {
        return new CustomFrameworkAdapter();
    }
    
    @Override
    public AlarmAdapter createAlarmAdapter() {
        return new CustomAlarmAdapter();
    }
    
    @Override
    public CertificateAdapter createCertificateAdapter() {
        return new CustomCertificateAdapter();
    }
    
    @Override
    public ServiceManagementAdapter createServiceManagementAdapter() {
        return new CustomServiceManagementAdapter();
    }
    
    @Override
    public SystemUtilAdapter createSystemUtilAdapter() {
        return new CustomSystemUtilAdapter();
    }
    
    @Override
    public ResourceMonitorAdapter createResourceMonitorAdapter() {
        return new CustomResourceMonitorAdapter();
    }
}
