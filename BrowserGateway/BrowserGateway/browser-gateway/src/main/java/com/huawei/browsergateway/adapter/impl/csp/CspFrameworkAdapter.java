package com.huawei.browsergateway.adapter.impl.csp;

import com.huawei.browsergateway.adapter.interfaces.FrameworkAdapter;
import com.huawei.csp.csejsdk.core.api.Framework;
import com.huawei.csp.om.transport.vertx.init.OmsdkStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 框架适配器 - CSP SDK实现
 * 适用场景：内网环境，使用华为CSP SDK
 */
@Component
public class CspFrameworkAdapter implements FrameworkAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CspFrameworkAdapter.class);
    
    private boolean isStarted = false;
    
    @Override
    public boolean start() {
        try {
            // 调用CSP SDK的Framework.start()启动CSE框架
            Framework.start();
            isStarted = true;
            logger.info("CSE Framework started successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to start CSE Framework", e);
            isStarted = false;
            return false;
        }
    }
    
    @Override
    public boolean stop() {
        // CSE框架通常不需要显式停止
        isStarted = false;
        logger.info("CSE Framework stopped");
        return true;
    }
    
    @Override
    public boolean initializeOmSdK() {
        try {
            // 调用CSP SDK的OmsdkStarter.omsdkInit()初始化OM SDK
            OmsdkStarter.omsdkInit();
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
