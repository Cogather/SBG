package com.huawei.browsergateway.adapter.factory;

import com.huawei.browsergateway.adapter.impl.csp.*;
import com.huawei.browsergateway.adapter.interfaces.*;
import org.springframework.stereotype.Component;

/**
 * CSP SDK适配器工厂
 * 创建使用CSP SDK的具体实现
 */
@Component
public class CspSdkAdapterFactory implements AdapterFactory {
    
    @Override
    public FrameworkAdapter createFrameworkAdapter() {
        return new CspFrameworkAdapter();
    }
    
    @Override
    public AlarmAdapter createAlarmAdapter() {
        return new CspAlarmAdapter();
    }
    
    @Override
    public CertificateAdapter createCertificateAdapter() {
        return new CspCertificateAdapter();
    }
    
    @Override
    public ServiceManagementAdapter createServiceManagementAdapter() {
        return new CspServiceManagementAdapter();
    }
    
    @Override
    public SystemUtilAdapter createSystemUtilAdapter() {
        return new CspSystemUtilAdapter();
    }
    
    @Override
    public ResourceMonitorAdapter createResourceMonitorAdapter() {
        return new CspResourceMonitorAdapter();
    }
}
