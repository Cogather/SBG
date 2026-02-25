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
 * CPU使用率检查策略
 */
@Component
public class CpuUsageCheck implements ICheckStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(CpuUsageCheck.class);
    
    private static final String STRATEGY_NAME = "CPU使用率检查";
    
    @Autowired
    private ResourceMonitorAdapter resourceMonitorAdapter;
    
    // CPU告警触发阈值（默认90%）
    @Value("${browsergw.healthCheck.cpu-trigger-threshold:90}")
    private float cpuTriggerThreshold;
    
    // CPU恢复阈值（默认80%）
    @Value("${browsergw.healthCheck.cpu-recover-threshold:80}")
    private float cpuRecoverThreshold;
    
    @Override
    public HealthCheckResult check() {
        try {
            // 获取CPU使用率
            float cpuUsage = resourceMonitorAdapter.getCpuUsage();
            
            // 获取详细统计信息
            ResourceStatistics stats = resourceMonitorAdapter.getStatistics("cpu");
            if (stats != null && stats.isSuccess()) {
                cpuUsage = stats.getRatio();
            }
            
            // 判断是否健康
            boolean healthy = cpuUsage < cpuRecoverThreshold;
            
            String message = healthy 
                ? String.format("CPU使用率正常: %.2f%%", cpuUsage)
                : String.format("CPU使用率过高: %.2f%%, 阈值: %.2f%%", cpuUsage, cpuTriggerThreshold);
            
            log.debug("CPU健康检查完成: usage={}%, healthy={}, threshold={}%", 
                cpuUsage, healthy, cpuTriggerThreshold);
            
            return HealthCheckResult.builder()
                .metricName("cpu_usage")
                .value(cpuUsage)
                .healthy(healthy)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .threshold(cpuTriggerThreshold)
                .recoverThreshold(cpuRecoverThreshold)
                .build();
                
        } catch (Exception e) {
            log.error("CPU健康检查异常", e);
            return HealthCheckResult.builder()
                .metricName("cpu_usage")
                .value(0.0f)
                .healthy(false)
                .message("CPU健康检查失败: " + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .threshold(cpuTriggerThreshold)
                .recoverThreshold(cpuRecoverThreshold)
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
        return !result.isHealthy() && result.getValue() >= cpuTriggerThreshold;
    }
    
    /**
     * 检查是否可以恢复告警
     * 
     * @return true表示可以恢复告警
     */
    public boolean shouldRecoverAlarm() {
        HealthCheckResult result = check();
        return result.isHealthy() && result.getValue() < cpuRecoverThreshold;
    }
}
