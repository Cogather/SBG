package com.huawei.browsergateway.adapter.impl.csp;

import com.huawei.browsergateway.adapter.interfaces.SystemUtilAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 系统工具适配器 - CSP SDK实现
 */
@Component("cspSystemUtilAdapter")
@ConditionalOnProperty(name = "adapter.provider.type", havingValue = "CSP_SDK", matchIfMissing = true)
public class CspSystemUtilAdapter implements SystemUtilAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CspSystemUtilAdapter.class);
    
    @Override
    public String getEnvString(String key, String defaultValue) {
        try {
            Class<?> systemUtilClass = Class.forName("com.huawei.csp.csejsdk.common.utils.SystemUtil");
            Method getStringFromEnv = systemUtilClass.getMethod("getStringFromEnv", String.class);
            String value = (String) getStringFromEnv.invoke(null, key);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            logger.warn("Failed to get env string from CSP SDK, using default value", e);
            return defaultValue;
        }
    }
    
    @Override
    public int getEnvInteger(String key, int defaultValue) {
        try {
            Class<?> systemUtilClass = Class.forName("com.huawei.csp.csejsdk.common.utils.SystemUtil");
            Method getIntFromEnv = systemUtilClass.getMethod("getIntFromEnv", String.class);
            Integer value = (Integer) getIntFromEnv.invoke(null, key);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            logger.warn("Failed to get env integer from CSP SDK, using default value", e);
            return defaultValue;
        }
    }
    
    @Override
    public long getEnvLong(String key, long defaultValue) {
        try {
            Class<?> systemUtilClass = Class.forName("com.huawei.csp.csejsdk.common.utils.SystemUtil");
            Method getLongFromEnv = systemUtilClass.getMethod("getLongFromEnv", String.class);
            Long value = (Long) getLongFromEnv.invoke(null, key);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            logger.warn("Failed to get env long from CSP SDK, using default value", e);
            return defaultValue;
        }
    }
    
    @Override
    public boolean getEnvBoolean(String key, boolean defaultValue) {
        try {
            Class<?> systemUtilClass = Class.forName("com.huawei.csp.csejsdk.common.utils.SystemUtil");
            Method getBooleanFromEnv = systemUtilClass.getMethod("getBooleanFromEnv", String.class);
            Boolean value = (Boolean) getBooleanFromEnv.invoke(null, key);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            logger.warn("Failed to get env boolean from CSP SDK, using default value", e);
            return defaultValue;
        }
    }
    
    @Override
    public void setEnv(String key, String value) {
        try {
            Class<?> systemUtilClass = Class.forName("com.huawei.csp.csejsdk.common.utils.SystemUtil");
            Method setEnv = systemUtilClass.getMethod("setEnv", String.class, String.class);
            setEnv.invoke(null, key, value);
        } catch (Exception e) {
            logger.warn("Failed to set env via CSP SDK", e);
        }
    }
}
