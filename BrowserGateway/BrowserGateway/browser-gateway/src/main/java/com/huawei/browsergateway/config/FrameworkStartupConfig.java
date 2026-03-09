package com.huawei.browsergateway.config;

import com.huawei.browsergateway.adapter.FrameworkAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 框架启动配置类
 * 提供CSE框架和OM SDK初始化方法
 */
public class FrameworkStartupConfig {

    private static final Logger logger = LogManager.getLogger(FrameworkStartupConfig.class);

    /**
     * 初始化框架
     * @param frameworkAdapter 框架适配器
     */
    public static void initializeFramework(FrameworkAdapter frameworkAdapter) {
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