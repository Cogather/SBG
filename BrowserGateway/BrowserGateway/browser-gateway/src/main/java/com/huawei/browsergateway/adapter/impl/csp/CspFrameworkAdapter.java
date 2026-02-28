package com.huawei.browsergateway.adapter.impl.csp;

import com.huawei.browsergateway.adapter.interfaces.FrameworkAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 框架适配器 - CSP SDK实现
 * 适用场景：内网环境，使用华为CSP SDK
 */
@Component("cspFrameworkAdapter")
@ConditionalOnProperty(name = "adapter.provider.type", havingValue = "CSP_SDK", matchIfMissing = true)
public class CspFrameworkAdapter implements FrameworkAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CspFrameworkAdapter.class);
    
    private boolean isStarted = false;
    
    @Override
    public boolean start() {
        try {
            // 使用反射调用，避免编译时依赖
            Class<?> frameworkClass = Class.forName("com.huawei.csp.csejsdk.core.api.Framework");
            frameworkClass.getMethod("start").invoke(null);
            isStarted = true;
            logger.info("CSE Framework started successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to start CSE Framework", e);
            return false;
        }
    }
    
    @Override
    public boolean stop() {
        isStarted = false;
        logger.info("CSE Framework stopped");
        return true;
    }
    
    @Override
    public boolean initializeOmSdK() {
        try {
            // 使用反射调用，避免编译时依赖
            Class<?> omsdkStarterClass = Class.forName("com.huawei.csp.om.transport.vertx.init.OmsdkStarter");
            omsdkStarterClass.getMethod("omsdkInit").invoke(null);
            logger.info("OM SDK initialized successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to initialize OM SDK", e);
            return false;
        }
    }
    
    @Override
    public boolean isStarted() {
        return isStarted;
    }
}
