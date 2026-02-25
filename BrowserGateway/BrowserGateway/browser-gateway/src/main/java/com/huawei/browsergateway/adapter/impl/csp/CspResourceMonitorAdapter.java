package com.huawei.browsergateway.adapter.impl.csp;

import com.huawei.browsergateway.adapter.dto.ResourceStatistics;
import com.huawei.browsergateway.adapter.interfaces.ResourceMonitorAdapter;
import com.huawei.csp.csejsdk.rssdk.api.RsApi;
import com.huawei.csp.csejsdk.rssdk.rspojo.RSPojo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 资源监控适配器 - CSP SDK实现
 * 适用场景：内网环境，调用RsApi.getLatestContainerResourceStatistics()
 */
@Component
public class CspResourceMonitorAdapter implements ResourceMonitorAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CspResourceMonitorAdapter.class);
    
    @Autowired(required = false)
    private RsApi rsApi;
    
    /**
     * 获取RsApi实例
     * 如果通过Spring注入失败，尝试其他方式获取
     */
    private RsApi getRsApi() {
        if (rsApi != null) {
            return rsApi;
        }
        // 如果Spring注入失败，尝试通过其他方式获取RsApi实例
        // 这里可能需要根据实际CSP SDK的实现方式调整
        // 例如：RsApi.getInstance() 或其他工厂方法
        logger.warn("RsApi not injected, resource monitoring may not work correctly");
        return null;
    }
    
    @Override
    public float getCpuUsage() {
        try {
            RsApi api = getRsApi();
            if (api == null) {
                logger.warn("RsApi not available, returning 0.0f for CPU usage");
                return 0.0f;
            }
            
            // 调用CSP SDK的RsApi查询CPU使用率
            RSPojo.APIBackConfig config = api.getLatestContainerResourceStatistics("cpu");
            if (config != null && config.isSuccess) {
                return config.ratio;
            } else {
                logger.warn("Failed to get CPU usage, config is null or not success");
                return 0.0f;
            }
        } catch (Exception e) {
            logger.error("Failed to get CPU usage", e);
            return 0.0f;
        }
    }
    
    @Override
    public float getMemoryUsage() {
        try {
            RsApi api = getRsApi();
            if (api == null) {
                logger.warn("RsApi not available, returning 0.0f for memory usage");
                return 0.0f;
            }
            
            // 调用CSP SDK的RsApi查询内存使用率
            RSPojo.APIBackConfig config = api.getLatestContainerResourceStatistics("memory");
            if (config != null && config.isSuccess) {
                return config.ratio;
            } else {
                logger.warn("Failed to get memory usage, config is null or not success");
                return 0.0f;
            }
        } catch (Exception e) {
            logger.error("Failed to get memory usage", e);
            return 0.0f;
        }
    }
    
    @Override
    public float getNetworkUsage() {
        try {
            RsApi api = getRsApi();
            if (api == null) {
                logger.warn("RsApi not available, returning 0.0f for network usage");
                return 0.0f;
            }
            
            // 调用CSP SDK查询网络使用率
            RSPojo.APIBackConfig config = api.getLatestContainerResourceStatistics("network");
            if (config != null && config.isSuccess) {
                return config.ratio;
            } else {
                logger.warn("Failed to get network usage, config is null or not success");
                return 0.0f;
            }
        } catch (Exception e) {
            logger.error("Failed to get network usage", e);
            return 0.0f;
        }
    }
    
    @Override
    public ResourceStatistics getStatistics(String metricType) {
        try {
            RsApi api = getRsApi();
            if (api == null) {
                logger.warn("RsApi not available, returning failed statistics");
                ResourceStatistics stats = new ResourceStatistics();
                stats.setSuccess(false);
                return stats;
            }
            
            // 调用CSP SDK获取资源统计信息
            RSPojo.APIBackConfig config = api.getLatestContainerResourceStatistics(metricType);
            
            if (config != null && config.isSuccess) {
                ResourceStatistics stats = new ResourceStatistics();
                stats.setSuccess(true);
                stats.setRatio(config.ratio);
                stats.setTimestamp(config.timestamp);
                stats.setAvailable(config.available);
                stats.setCapacity(config.capacity);
                return stats;
            } else {
                logger.warn("Failed to get statistics for metric: {}, config is null or not success", metricType);
                ResourceStatistics stats = new ResourceStatistics();
                stats.setSuccess(false);
                return stats;
            }
        } catch (Exception e) {
            logger.error("Failed to get statistics for: {}", metricType, e);
            ResourceStatistics stats = new ResourceStatistics();
            stats.setSuccess(false);
            return stats;
        }
    }
}
