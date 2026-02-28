package com.huawei.browsergateway.config;

import com.huawei.browsergateway.adapter.interfaces.FrameworkAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 框架启动配置类
 * 在Spring Boot应用启动后自动初始化CSE框架和OM SDK
 */
@Component
public class FrameworkStartupConfig implements ApplicationRunner {
    
    private static final Logger logger = LogManager.getLogger(FrameworkStartupConfig.class);
    
    @Autowired(required = false)
    private FrameworkAdapter frameworkAdapter;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (frameworkAdapter == null) {
            logger.warn("FrameworkAdapter is not available, skipping framework initialization");
            return;
        }
        
        logger.info("Starting CSE Framework...");
        boolean started = frameworkAdapter.start();
        if (started) {
            logger.info("CSE Framework started successfully");
        } else {
            logger.error("Failed to start CSE Framework");
        }
        
        logger.info("Initializing OM SDK...");
        boolean initialized = frameworkAdapter.initializeOmSdK();
        if (initialized) {
            logger.info("OM SDK initialized successfully");
        } else {
            logger.error("Failed to initialize OM SDK");
        }
    }
}
