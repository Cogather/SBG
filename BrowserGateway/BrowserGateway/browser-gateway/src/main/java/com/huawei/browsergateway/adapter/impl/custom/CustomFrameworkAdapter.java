package com.huawei.browsergateway.adapter.impl.custom;

import com.huawei.browsergateway.adapter.FrameworkAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * 框架适配器 - 自定义实现
 * 适用场景：外网环境，使用本地逻辑替代CSP SDK
 */
public class CustomFrameworkAdapter implements FrameworkAdapter {
    
    private static final Logger logger = LogManager.getLogger(CustomFrameworkAdapter.class);

    private boolean isStarted = false;
    
    @Override
    public boolean start() {
        // 本地环境无需启动CSE框架
        isStarted = true;
        logger.info("Custom Framework started (no-op for external environment)");
        return true;
    }
    
    @Override
    public boolean stop() {
        isStarted = false;
        logger.info("Custom Framework stopped");
        return true;
    }
    
    @Override
    public boolean initializeOmSdK() {
        // 本地环境无需初始化OM SDK
        logger.info("Custom OM SDK initialized (no-op for external environment)");
        return true;
    }
    
    @Override
    public boolean isStarted() {
        return isStarted;
    }
}
