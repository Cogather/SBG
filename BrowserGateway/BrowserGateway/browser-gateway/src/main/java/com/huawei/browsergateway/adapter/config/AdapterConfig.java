package com.huawei.browsergateway.adapter.config;

import com.huawei.browsergateway.adapter.*;
import com.huawei.browsergateway.adapter.factory.AdapterFactory;
import com.huawei.browsergateway.adapter.factory.CspAdapterFactory;
import com.huawei.browsergateway.adapter.factory.CustomAdapterFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 适配器配置类
 * 根据环境配置自动选择合适的适配器工厂
 */
@Configuration
public class AdapterConfig {
    private static final Logger log = LogManager.getLogger(AdapterConfig.class);

    /**
     * 环境配置：internal(内网) 或 external(外网)
     * 默认为内网环境
     */
    @Value("${csp.adapter.environment:external}")
    private String environment;

    @Bean
    public AdapterFactory adapterFactory() {
        log.info("Initializing adapter factory for environment: {}", environment);

        if ("external".equalsIgnoreCase(environment)) {
            log.info("Using CustomAdapterFactory for external environment");
            return new CustomAdapterFactory();
        } else {
            log.info("Using CspAdapterFactory for internal environment");
            return new CspAdapterFactory();
        }
    }

    @Bean
    public FrameworkAdapter frameworkAdapter(AdapterFactory factory) {
        return factory.createFrameworkAdapter();
    }

    @Bean
    public AlarmAdapter alarmAdapter(AdapterFactory factory) {
        return factory.createAlarmAdapter();
    }

    @Bean
    public CertificateAdapter certificateAdapter(AdapterFactory factory) {
        return factory.createCertificateAdapter();
    }

    @Bean
    public ServiceManagementAdapter serviceManagementAdapter(AdapterFactory factory) {
        return factory.createServiceManagementAdapter();
    }

    @Bean
    public SystemUtilAdapter systemUtilAdapter(AdapterFactory factory) {
        return factory.createSystemUtilAdapter();
    }

    @Bean
    public ResourceMonitorAdapter resourceMonitorAdapter(AdapterFactory factory) {
        return factory.createResourceMonitorAdapter();
    }

    @Bean
    public AuditLogAdapter auditLogAdapter(AdapterFactory factory) {
        return factory.createAuditLogAdapter();
    }

    @Bean
    public com.huawei.browsergateway.util.DeployUtil deployUtil(SystemUtilAdapter systemUtilAdapter) {
        return new com.huawei.browsergateway.util.DeployUtil(systemUtilAdapter);
    }

    /**
     * 获取适配器环境配置
     * 用于非Spring容器环境（如main方法）
     */
    public static String getAdapterEnvironment() {
        // 从系统属性或环境变量中获取，默认为internal
        String env = System.getProperty("csp.adapter.environment");
        if (env == null) {
            env = System.getenv("CSP_ADAPTER_ENVIRONMENT");
        }
        return env != null ? env : "internal";
    }

    /**
     * 根据环境获取适配器工厂
     * 用于非Spring容器环境（如main方法）
     */
    public static AdapterFactory getAdapterFactory(String environment) {
        if ("external".equalsIgnoreCase(environment)) {
            log.info("Using CustomAdapterFactory for external environment");
            return new CustomAdapterFactory();
        } else {
            log.info("Using CspAdapterFactory for internal environment");
            return new CspAdapterFactory();
        }
    }
}
