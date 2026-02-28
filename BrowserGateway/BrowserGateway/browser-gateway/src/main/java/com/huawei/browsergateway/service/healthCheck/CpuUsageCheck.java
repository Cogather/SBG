package com.huawei.browsergateway.service.healthCheck;

import com.huawei.browsergateway.adapter.dto.ResourceStatistics;
import com.huawei.browsergateway.adapter.interfaces.ResourceMonitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * CPU使用率检查策略
 */
@Component
public class CpuUsageCheck implements ICheckStrategy {
    private static final Logger log = LoggerFactory.getLogger(CpuUsageCheck.class);
    private boolean inHighUsage = false;
    private final float triggerThreshold;
    private final float recoverThreshold;
    
    @Autowired
    private ResourceMonitorAdapter resourceMonitorAdapter;

    public CpuUsageCheck(float triggerThreshold, float recoverThreshold) {
        this.triggerThreshold = triggerThreshold;
        this.recoverThreshold = recoverThreshold;
        log.info("start to check cpu usage, triggerThreshold: {}, recoverThreshold:{}", triggerThreshold, recoverThreshold);
    }

    @Override
    public HealthCheckResult check() {
        HealthCheckResult result = new HealthCheckResult();
        result.setCheckItem("CpuUsageCheck");
        result.setHealthy(true);
        
        if (resourceMonitorAdapter == null) {
            log.warn("ResourceMonitorAdapter is not available");
            return result;
        }
        
        ResourceStatistics cpuStatistics = resourceMonitorAdapter.getStatistics("cpu");
        if (!cpuStatistics.isSuccess()) {
            return result;
        }
        
        if (inHighUsage) {
            result.setHealthy(cpuStatistics.getRatio() < recoverThreshold);
        } else {
            result.setHealthy(cpuStatistics.getRatio() < triggerThreshold);
        }
        inHighUsage = !result.isHealthy();
        if (inHighUsage) {
            result.setErrorMsg(String.format("cpu in high usage[%f];", cpuStatistics.getRatio()));
        }
        return result;
    }
}