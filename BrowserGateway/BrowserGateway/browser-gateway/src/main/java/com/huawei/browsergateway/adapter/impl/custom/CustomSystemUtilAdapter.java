package com.huawei.browsergateway.adapter.impl.custom;

import com.huawei.browsergateway.adapter.SystemUtilAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统工具适配器 - 自定义实现
 */
public class CustomSystemUtilAdapter implements SystemUtilAdapter {
    
    private static final Logger logger = LogManager.getLogger(CustomSystemUtilAdapter.class);
    
    private final Map<String, String> mockEnvVars = new ConcurrentHashMap<>();

    @Override
    public String getEnvString(String key, String defaultValue) {
        // 首先检查mock环境变量
        String value = mockEnvVars.get(key);
        if (value != null) {
            return value;
        }

        // 检查系统环境变量
        value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        return defaultValue;
    }
    
    @Override
    public int getEnvInteger(String key, int defaultValue) {
        String value = getEnvString(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse integer from env {}: {}", key, value);
            return defaultValue;
        }
    }
    
    @Override
    public long getEnvLong(String key, long defaultValue) {
        String value = getEnvString(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse long from env {}: {}", key, value);
            return defaultValue;
        }
    }
    
    @Override
    public boolean getEnvBoolean(String key, boolean defaultValue) {
        String value = getEnvString(key, null);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    @Override
    public void setEnv(String key, String value) {
        mockEnvVars.put(key, value);
    }
}
