package com.huawei.browsergateway.adapter.factory;

import com.huawei.browsergateway.adapter.*;
import com.huawei.browsergateway.adapter.impl.csp.*;

/**
 * CSP SDK适配器工厂
 * 职责：创建使用CSP SDK的适配器实例（内网环境）
 */
public class CspAdapterFactory implements AdapterFactory {

    @Override
    public FrameworkAdapter createFrameworkAdapter() {
        return new CspFrameworkAdapter();
    }

    @Override
    public AlarmAdapter createAlarmAdapter() {
        return new CspAlarmAdapter(new com.huawei.browsergateway.util.DeployUtil(createSystemUtilAdapter()));
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

    @Override
    public AuditLogAdapter createAuditLogAdapter() {
        return new CspAuditLogAdapter();
    }
}
