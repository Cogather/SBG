package com.huawei.browsergateway.service.healthCheck;

import com.huawei.browsergateway.adapter.dto.ResourceStatistics;
import com.huawei.browsergateway.adapter.interfaces.ResourceMonitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 内存使用率检查策略
 */
@Component
public class MemoryUsageCheck implements ICheckStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(MemoryUsageCheck.class);
    
    private boolean inHighUsage = false;
    private final float triggerThreshold;
    private final float recoveryThreshold;
    
    @Autowired
    private ResourceMonitorAdapter resourceMonitorAdapter;

    public MemoryUsageCheck(float triggerThreshold, float recoveryThreshold) {
        this.triggerThreshold = triggerThreshold;
        this.recoveryThreshold = recoveryThreshold;
        log.info("start to check memory usage, triggerThreshold: {}, recoveryThreshold: {}", triggerThreshold, recoveryThreshold);
    }

    @Override
    public HealthCheckResult check() {
        HealthCheckResult result = new HealthCheckResult();
        result.setCheckItem("MemoryUsageCheck");
        result.setHealthy(true);
        
        if (resourceMonitorAdapter == null) {
            log.warn("ResourceMonitorAdapter is not available");
            return result;
        }
        
        ResourceStatistics memoryStatistic = resourceMonitorAdapter.getStatistics("memory");
        if (!memoryStatistic.isSuccess()) {
            return result;
        }
        
        if (inHighUsage) {
            result.setHealthy(memoryStatistic.getRatio() < recoveryThreshold);
        } else {
            result.setHealthy(memoryStatistic.getRatio() < triggerThreshold);
        }
        inHighUsage = !result.isHealthy();
        if (inHighUsage) {
            result.setErrorMsg(String.format("memory in high usage[%f];", memoryStatistic.getRatio()));
        }
        return result;
    }
}