package com.huawei.browsergateway.adapter.factory;

import com.huawei.browsergateway.adapter.*;
import com.huawei.browsergateway.adapter.impl.custom.*;

/**
 * 自定义适配器工厂
 * 职责：创建自定义实现的适配器实例（外网环境）
 */
public class CustomAdapterFactory implements AdapterFactory {

    @Override
    public FrameworkAdapter createFrameworkAdapter() {
        return new CustomFrameworkAdapter();
    }

    @Override
    public AlarmAdapter createAlarmAdapter() {
        return new CustomAlarmAdapter(new com.huawei.browsergateway.util.DeployUtil(createSystemUtilAdapter()));
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

    @Override
    public AuditLogAdapter createAuditLogAdapter() {
        return new CustomAuditLogAdapter();
    }
}
