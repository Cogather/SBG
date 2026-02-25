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
 * 内存使用率检查策略
 */
@Component
public class MemoryUsageCheck implements ICheckStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(MemoryUsageCheck.class);
    
    private static final String STRATEGY_NAME = "内存使用率检查";
    
    @Autowired
    private ResourceMonitorAdapter resourceMonitorAdapter;
    
    // 内存告警触发阈值（默认90%）
    @Value("${browsergw.healthCheck.memory-trigger-threshold:90}")
    private float memoryTriggerThreshold;
    
    // 内存恢复阈值（默认80%）
    @Value("${browsergw.healthCheck.memory-recover-threshold:80}")
    private float memoryRecoverThreshold;
    
    @Override
    public HealthCheckResult check() {
        try {
            // 获取内存使用率
            float memoryUsage = resourceMonitorAdapter.getMemoryUsage();
            
            // 获取详细统计信息
            ResourceStatistics stats = resourceMonitorAdapter.getStatistics("memory");
            if (stats != null && stats.isSuccess()) {
                memoryUsage = stats.getRatio();
            }
            
            // 判断是否健康
            boolean healthy = memoryUsage < memoryRecoverThreshold;
            
            String message = healthy
                ? String.format("内存使用率正常: %.2f%%", memoryUsage)
                : String.format("内存使用率过高: %.2f%%, 阈值: %.2f%%", memoryUsage, memoryTriggerThreshold);
            
            log.debug("内存健康检查完成: usage={}%, healthy={}, threshold={}%", 
                memoryUsage, healthy, memoryTriggerThreshold);
            
            return HealthCheckResult.builder()
                .metricName("memory_usage")
                .value(memoryUsage)
                .healthy(healthy)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .threshold(memoryTriggerThreshold)
                .recoverThreshold(memoryRecoverThreshold)
                .build();
                
        } catch (Exception e) {
            log.error("内存健康检查异常", e);
            return HealthCheckResult.builder()
                .metricName("memory_usage")
                .value(0.0f)
                .healthy(false)
                .message("内存健康检查失败: " + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .threshold(memoryTriggerThreshold)
                .recoverThreshold(memoryRecoverThreshold)
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
        return !result.isHealthy() && result.getValue() >= memoryTriggerThreshold;
    }
    
    /**
     * 检查是否可以恢复告警
     * 
     * @return true表示可以恢复告警
     */
    public boolean shouldRecoverAlarm() {
        HealthCheckResult result = check();
        return result.isHealthy() && result.getValue() < memoryRecoverThreshold;
    }
}
