package com.huawei.browsergateway.config;

import com.huawei.browsergateway.service.ICse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * local profile 下的 ICse 实现配置
 * 直接返回 application-local.yaml 中配置的 GIDS 地址，无需 K8s 服务发现
 */
@Configuration
@Profile("local")
public class LocalCseConfig {

    @Value("${gids.endpoint:localhost:9090}")
    private String gidsEndpoint;

    @Bean
    @Primary
    public ICse localCse() {
        return new ICse() {
            @Override
            public String getReportEndpoint() {
                return gidsEndpoint;
            }
        };
    }
}
