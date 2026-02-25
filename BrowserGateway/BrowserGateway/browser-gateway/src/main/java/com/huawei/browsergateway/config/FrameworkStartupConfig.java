package com.huawei.browsergateway.config;

import com.huawei.browsergateway.adapter.interfaces.FrameworkAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 框架启动配置类
 * 在Spring Boot应用启动完成后，初始化CSE框架和OM SDK
 */
@Component
public class FrameworkStartupConfig implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(FrameworkStartupConfig.class);
    
    @Autowired(required = false)
    private FrameworkAdapter frameworkAdapter;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (frameworkAdapter == null) {
            log.warn("FrameworkAdapter not found, skipping framework initialization");
            return;
        }
        
        log.info("Starting framework initialization...");
        
        // 1. 启动CSE框架
        boolean frameworkStarted = frameworkAdapter.start();
        if (!frameworkStarted) {
            log.error("Failed to start CSE Framework, but continuing application startup");
        }
        
        // 2. 初始化OM SDK
        boolean omSdkInitialized = frameworkAdapter.initializeOmSdK();
        if (!omSdkInitialized) {
            log.error("Failed to initialize OM SDK, but continuing application startup");
        }
        
        if (frameworkStarted && omSdkInitialized) {
            log.info("Framework initialization completed successfully");
        } else {
            log.warn("Framework initialization completed with errors");
        }
    }
}
