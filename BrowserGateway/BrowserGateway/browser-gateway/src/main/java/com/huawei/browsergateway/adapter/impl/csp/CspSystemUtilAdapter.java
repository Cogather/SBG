package com.huawei.browsergateway.adapter.impl.csp;

import com.huawei.browsergateway.adapter.interfaces.SystemUtilAdapter;
import com.huawei.csp.csejsdk.common.utils.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 系统工具适配器 - CSP SDK实现
 * 适用场景：内网环境，调用SystemUtil.getStringFromEnv()
 */
@Component
public class CspSystemUtilAdapter implements SystemUtilAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CspSystemUtilAdapter.class);
    
    @Override
    public String getEnvString(String key, String defaultValue) {
        try {
            // 调用CSP SDK的SystemUtil.getStringFromEnv()
            String value = SystemUtil.getStringFromEnv(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
            // 如果CSP SDK返回空，尝试使用系统环境变量作为降级
            value = System.getenv(key);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            logger.error("Failed to get env string: {}", key, e);
            // 降级到系统环境变量
            String value = System.getenv(key);
            return value != null ? value : defaultValue;
        }
    }
    
    @Override
    public int getEnvInteger(String key, int defaultValue) {
        try {
            String value = getEnvString(key, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.error("Failed to parse env integer: {}", key, e);
            return defaultValue;
        }
    }
    
    @Override
    public void setEnv(String key, String value) {
        // CSP SDK实现不支持设置环境变量
        logger.warn("setEnv is not supported in CSP SDK implementation");
    }
}
