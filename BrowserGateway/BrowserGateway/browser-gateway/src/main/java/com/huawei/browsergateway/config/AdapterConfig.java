package com.huawei.browsergateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 适配器配置类
 */
@Configuration
@ConfigurationProperties(prefix = "adapter")
@Data
public class AdapterConfig {
    
    private Provider provider = new Provider();
    
    @Data
    public static class Provider {
        private Type type = Type.CSP_SDK;
        private boolean enableMock = false;
        
        public enum Type {
            CSP_SDK,
            CUSTOM
        }
    }
}
