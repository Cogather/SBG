package com.huawei.browsergateway.service.healthCheck;

import com.huawei.browsergateway.adapter.dto.ResourceStatistics;
import com.huawei.browsergateway.adapter.interfaces.ResourceMonitorAdapter;
import com.huawei.browsergateway.service.healthCheck.dto.HealthCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 网络使用率检查策略
 */
@Component
public class NetworkUsageCheck implements ICheckStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(NetworkUsageCheck.class);
    
    private static final String STRATEGY_NAME = "网络使用率检查";
    
    @Autowired
    private ResourceMonitorAdapter resourceMonitorAdapter;
    
    // 网络告警触发阈值（默认85%）
    @Value("${browsergw.healthCheck.network-trigger-threshold:85}")
    private float networkTriggerThreshold;
    
    // 网络恢复阈值（默认75%）
    @Value("${browsergw.healthCheck.network-recover-threshold:75}")
    private float networkRecoverThreshold;
    
    @Override
    public HealthCheckResult check() {
        try {
            // 获取网络使用率
            float networkUsage = resourceMonitorAdapter.getNetworkUsage();
            
            // 获取详细统计信息
            ResourceStatistics stats = resourceMonitorAdapter.getStatistics("network");
            if (stats != null && stats.isSuccess()) {
                networkUsage = stats.getRatio();
            }
            
            // 判断是否健康
            boolean healthy = networkUsage < networkRecoverThreshold;
            
            String message = healthy
                ? String.format("网络使用率正常: %.2f%%", networkUsage)
                : String.format("网络使用率过高: %.2f%%, 阈值: %.2f%%", networkUsage, networkTriggerThreshold);
            
            log.debug("网络健康检查完成: usage={}%, healthy={}, threshold={}%", 
                networkUsage, healthy, networkTriggerThreshold);
            
            return HealthCheckResult.builder()
                .metricName("network_usage")
                .value(networkUsage)
                .healthy(healthy)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .threshold(networkTriggerThreshold)
                .recoverThreshold(networkRecoverThreshold)
                .build();
                
        } catch (Exception e) {
            log.error("网络健康检查异常", e);
            return HealthCheckResult.builder()
                .metricName("network_usage")
                .value(0.0f)
                .healthy(false)
                .message("网络健康检查失败: " + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .threshold(networkTriggerThreshold)
                .recoverThreshold(networkRecoverThreshold)
                .build();
        }
    }
    
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    /**
     * 检查是否需要触发告警
     * 
     * @return true表示需要触发告警
     */
    public boolean shouldTriggerAlarm() {
        HealthCheckResult result = check();
        return !result.isHealthy() && result.getValue() >= networkTriggerThreshold;
    }
    
    /**
     * 检查是否可以恢复告警
     * 
     * @return true表示可以恢复告警
     */
    public boolean shouldRecoverAlarm() {
        HealthCheckResult result = check();
        return result.isHealthy() && result.getValue() < networkRecoverThreshold;
    }
}
