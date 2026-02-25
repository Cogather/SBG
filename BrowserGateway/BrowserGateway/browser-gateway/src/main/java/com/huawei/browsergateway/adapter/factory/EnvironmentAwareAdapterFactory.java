package com.huawei.browsergateway.adapter.factory;

import com.huawei.browsergateway.adapter.interfaces.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 环境感知适配器工厂
 * 根据配置自动选择CSP SDK实现或自定义实现
 */
@Component
public class EnvironmentAwareAdapterFactory implements AdapterFactory {
    
    @Value("${adapter.provider.type:CUSTOM}")
    private AdapterProviderType providerType;
    
    @Autowired(required = false)
    private AdapterFactory cspSdkAdapterFactory; // 使用接口类型，避免直接依赖 CspSdkAdapterFactory
    
    private final CustomAdapterFactory customAdapterFactory;
    
    public EnvironmentAwareAdapterFactory(CustomAdapterFactory customAdapterFactory) {
        this.customAdapterFactory = customAdapterFactory;
    }
    
    @Override
    public FrameworkAdapter createFrameworkAdapter() {
        return getFactory().createFrameworkAdapter();
    }
    
    @Override
    public AlarmAdapter createAlarmAdapter() {
        return getFactory().createAlarmAdapter();
    }
    
    @Override
    public CertificateAdapter createCertificateAdapter() {
        return getFactory().createCertificateAdapter();
    }
    
    @Override
    public ServiceManagementAdapter createServiceManagementAdapter() {
        return getFactory().createServiceManagementAdapter();
    }
    
    @Override
    public SystemUtilAdapter createSystemUtilAdapter() {
        return getFactory().createSystemUtilAdapter();
    }
    
    @Override
    public ResourceMonitorAdapter createResourceMonitorAdapter() {
        return getFactory().createResourceMonitorAdapter();
    }
    
    private AdapterFactory getFactory() {
        switch (providerType) {
            case CSP_SDK:
                if (cspSdkAdapterFactory == null) {
                    // 外网环境，CSP SDK 不可用，回退到自定义实现
                    return customAdapterFactory;
                }
                return cspSdkAdapterFactory;
            case CUSTOM:
                return customAdapterFactory;
            default:
                // 默认使用自定义实现（外网环境）
                return customAdapterFactory;
        }
    }
    
    public enum AdapterProviderType {
        CSP_SDK,  // CSP SDK实现
        CUSTOM    // 自定义实现
    }
}
