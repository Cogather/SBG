package com.huawei.browsergateway.adapter.impl.custom;

import com.huawei.browsergateway.adapter.interfaces.SystemUtilAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统工具适配器 - 自定义实现
 * 适用场景：外网环境，优先从内存Map获取，其次从系统环境变量获取
 */
@Component
public class CustomSystemUtilAdapter implements SystemUtilAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomSystemUtilAdapter.class);
    
    private final Map<String, String> envMap = new ConcurrentHashMap<>();
    
    @Override
    public String getEnvString(String key, String defaultValue) {
        // 优先从内存Map获取
        String value = envMap.get(key);
        if (value != null) {
            return value;
        }
        
        // 其次从系统环境变量获取
        value = System.getenv(key);
        return value != null ? value : defaultValue;
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
        envMap.put(key, value);
    }
}
