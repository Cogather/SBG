package com.huawei.browsergateway.config;

import com.huawei.browsergateway.adapter.interfaces.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 适配器Bean配置类
 * 根据配置自动选择CSP SDK实现或Custom实现
 */
@Configuration
public class AdapterBeanConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(AdapterBeanConfiguration.class);
    
    /**
     * 框架适配器Bean - 通过条件注解自动选择
     */
    @Bean
    @Primary
    public FrameworkAdapter frameworkAdapter(
            @Autowired(required = false) com.huawei.browsergateway.adapter.impl.csp.CspFrameworkAdapter cspAdapter,
            @Autowired(required = false) com.huawei.browsergateway.adapter.impl.custom.CustomFrameworkAdapter customAdapter) {
        if (cspAdapter != null) {
            logger.info("Using CSP SDK FrameworkAdapter");
            return cspAdapter;
        } else if (customAdapter != null) {
            logger.info("Using Custom FrameworkAdapter");
            return customAdapter;
        } else {
            throw new IllegalStateException("No FrameworkAdapter implementation found");
        }
    }
    
    /**
     * 告警适配器Bean
     */
    @Bean
    @Primary
    public AlarmAdapter alarmAdapter(
            @Autowired(required = false) com.huawei.browsergateway.adapter.impl.csp.CspAlarmAdapter cspAdapter,
            @Autowired(required = false) com.huawei.browsergateway.adapter.impl.custom.CustomAlarmAdapter customAdapter) {
        if (cspAdapter != null) {
            logger.info("Using CSP SDK AlarmAdapter");
            return cspAdapter;
        } else if (customAdapter != null) {
            logger.info("Using Custom AlarmAdapter");
            return customAdapter;
        } else {
            throw new IllegalStateException("No AlarmAdapter implementation found");
        }
    }
    
    /**
     * 证书适配器Bean
     */
    @Bean
    @Primary
    public CertificateAdapter certificateAdapter(
            @Autowired(required = false) com.huawei.browsergateway.adapter.impl.csp.CspCertificateAdapter cspAdapter,
            @Autowired(required = false) com.huawei.browsergateway.adapter.impl.custom.CustomCertificateAdapter customAdapter) {
        if (cspAdapter != null) {
            logger.info("Using CSP SDK CertificateAdapter");
            return cspAdapter;
        } else if (customAdapter != null) {
            logger.info("Using Custom CertificateAdapter");
            return customAdapter;
        } else {
            throw new IllegalStateException("No CertificateAdapter implementation found");
        }
    }
    
    /**
     * 服务管理适配器Bean
     */
    @Bean
    @Primary
    public ServiceManagementAdapter serviceManagementAdapter(
            @Autowired(required = false) com.huawei.browsergateway.adapter.impl.csp.CspServiceManagementAdapter cspAdapter,
            @Autowired(required = false) com.huawei.browsergateway.adapter.impl.custom.CustomServiceManagementAdapter customAdapter) {
        if (cspAdapter != null) {
            logger.info("Using CSP SDK ServiceManagementAdapter");
            return cspAdapter;
        } else if (customAdapter != null) {
            logger.info("Using Custom ServiceManagementAdapter");
            return customAdapter;
        } else {
            throw new IllegalStateException("No ServiceManagementAdapter implementation found");
        }
    }
    
    /**
     * 系统工具适配器Bean
     */
    @Bean
    @Primary
    public SystemUtilAdapter systemUtilAdapter(
            @Autowired(required = false) com.huawei.browsergateway.adapter.impl.csp.CspSystemUtilAdapter cspAdapter,
            @Autowired(required = false) com.huawei.browsergateway.adapter.impl.custom.CustomSystemUtilAdapter customAdapter) {
        if (cspAdapter != null) {
            logger.info("Using CSP SDK SystemUtilAdapter");
            return cspAdapter;
        } else if (customAdapter != null) {
            logger.info("Using Custom SystemUtilAdapter");
            return customAdapter;
        } else {
            throw new IllegalStateException("No SystemUtilAdapter implementation found");
        }
    }
    
    /**
     * 资源监控适配器Bean
     */
    @Bean
    @Primary
    public ResourceMonitorAdapter resourceMonitorAdapter(
            @Autowired(required = false) com.huawei.browsergateway.adapter.impl.csp.CspResourceMonitorAdapter cspAdapter,
            @Autowired(required = false) com.huawei.browsergateway.adapter.impl.custom.CustomResourceMonitorAdapter customAdapter) {
        if (cspAdapter != null) {
            logger.info("Using CSP SDK ResourceMonitorAdapter");
            return cspAdapter;
        } else if (customAdapter != null) {
            logger.info("Using Custom ResourceMonitorAdapter");
            return customAdapter;
        } else {
            throw new IllegalStateException("No ResourceMonitorAdapter implementation found");
        }
    }
}
