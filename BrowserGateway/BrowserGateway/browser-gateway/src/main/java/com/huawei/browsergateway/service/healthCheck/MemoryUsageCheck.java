package com.huawei.browsergateway.service.healthCheck;

import com.huawei.browsergateway.adapter.ResourceMonitorAdapter;
import com.huawei.browsergateway.adapter.dto.ResourceStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MemoryUsageCheck implements ICheckStrategy {
    private static final Logger log = LogManager.getLogger(MemoryUsageCheck.class);
    private boolean inHighUsage = false;
    private final float triggerThreshold;
    private final float recoverThreshold;
    private final ResourceMonitorAdapter resourceMonitorAdapter;

    public MemoryUsageCheck(float triggerThreshold, float recoverThreshold, ResourceMonitorAdapter resourceMonitorAdapter) {
        this.triggerThreshold = triggerThreshold;
        this.recoverThreshold = recoverThreshold;
        this.resourceMonitorAdapter = resourceMonitorAdapter;
        log.info("start to check memory usage, triggerThreshold: {}, recoverThreshold:{}", triggerThreshold, recoverThreshold);
    }

    @Override
    public HealthCheckResult check() {
        HealthCheckResult result = new HealthCheckResult();
        result.setCheckItem("MemoryUsageCheck");
        result.setHealthy(true);
        ResourceStatistics memoryStatistics = resourceMonitorAdapter.getStatistics("memory");
        if (!memoryStatistics.isSuccess()) {
            return result;
        }
        if (inHighUsage) {
            result.setHealthy(memoryStatistics.getRatio() < recoverThreshold);
        } else {
            result.setHealthy(memoryStatistics.getRatio() < triggerThreshold);
        }
        inHighUsage = !result.isHealthy();
        if (inHighUsage) {
            result.setErrorMsg(String.format("memory in high usage[%f];", memoryStatistics.getRatio()));
        }
        return result;
    }

}