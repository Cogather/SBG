package com.huawei.browsergateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BrowserGateway 应用主类
 */
@SpringBootApplication
@EnableScheduling
public class BrowserGatewayApplication {
    
    private static final Logger log = LoggerFactory.getLogger(BrowserGatewayApplication.class);
    
    public static void main(String[] args) {
        // 注意：CSE框架和OM SDK的初始化已通过FrameworkStartupConfig自动处理
        // FrameworkStartupConfig会在Spring Boot应用启动完成后自动调用FrameworkAdapter.start()和initializeOmSdK()
        
        SpringApplication.run(BrowserGatewayApplication.class, args);
        log.info("BrowserGateway application started");
    }
}
