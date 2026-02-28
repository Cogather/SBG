package com.huawei.browsergateway.adapter.impl.csp;

import com.huawei.browsergateway.adapter.dto.ResourceStatistics;
import com.huawei.browsergateway.adapter.interfaces.ResourceMonitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 资源监控适配器 - CSP SDK实现
 */
@Component("cspResourceMonitorAdapter")
@ConditionalOnProperty(name = "adapter.provider.type", havingValue = "CSP_SDK", matchIfMissing = true)
public class CspResourceMonitorAdapter implements ResourceMonitorAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CspResourceMonitorAdapter.class);
    
    @Override
    public float getCpuUsage() {
        ResourceStatistics stats = getStatistics("cpu");
        return stats != null ? stats.getRatio() : 0.0f;
    }
    
    @Override
    public float getMemoryUsage() {
        ResourceStatistics stats = getStatistics("memory");
        return stats != null ? stats.getRatio() : 0.0f;
    }
    
    @Override
    public float getNetworkUsage() {
        ResourceStatistics stats = getStatistics("network");
        return stats != null ? stats.getRatio() : 0.0f;
    }
    
    @Override
    public ResourceStatistics getStatistics(String metricType) {
        try {
            Class<?> rsApiClass = Class.forName("com.huawei.csp.csejsdk.resdk.api.RsApi");
            Method getLatestContainerResourceStatistics = rsApiClass.getMethod("getLatestContainerResourceStatistics", String.class);
            Object result = getLatestContainerResourceStatistics.invoke(null, metricType);
            
            if (result == null) {
                return createEmptyStatistics();
            }
            
            // 使用反射获取字段值
            Class<?> resultClass = result.getClass();
            Method isSuccessMethod = resultClass.getMethod("isSuccess");
            Method getRatioMethod = resultClass.getMethod("getRatio");
            
            ResourceStatistics stats = new ResourceStatistics();
            stats.setSuccess((Boolean) isSuccessMethod.invoke(result));
            stats.setRatio(((Number) getRatioMethod.invoke(result)).floatValue());
            stats.setTimestamp(System.currentTimeMillis());
            
            return stats;
        } catch (Exception e) {
            logger.error("Failed to get resource statistics from CSP SDK", e);
            return createEmptyStatistics();
        }
    }
    
    private ResourceStatistics createEmptyStatistics() {
        ResourceStatistics stats = new ResourceStatistics();
        stats.setSuccess(false);
        stats.setRatio(0.0f);
        stats.setTimestamp(System.currentTimeMillis());
        return stats;
    }
}
