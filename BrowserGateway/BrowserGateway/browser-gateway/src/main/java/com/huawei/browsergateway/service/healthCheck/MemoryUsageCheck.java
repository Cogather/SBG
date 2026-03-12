package com.huawei.browsergateway.service.healthCheck;

import com.huawei.browsergateway.adapter.ResourceMonitorAdapter;
import com.huawei.browsergateway.adapter.dto.ResourceStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 内存使用率健康检查策略。
 * 采用滞回阈值：超过 triggerThreshold 触发告警，低于 recoverThreshold 才恢复正常。
 */
public class MemoryUsageCheck implements ICheckStrategy {

    private static final Logger log = LogManager.getLogger(MemoryUsageCheck.class);

    private final float triggerThreshold;
    private final float recoverThreshold;
    private final ResourceMonitorAdapter resourceMonitorAdapter;

    /** 当前是否处于高内存占用状态（用于滞回判断） */
    private boolean inHighUsage = false;

    public MemoryUsageCheck(float triggerThreshold, float recoverThreshold,
                            ResourceMonitorAdapter resourceMonitorAdapter) {
        this.triggerThreshold = triggerThreshold;
        this.recoverThreshold = recoverThreshold;
        this.resourceMonitorAdapter = resourceMonitorAdapter;
        log.info("start to check memory usage, triggerThreshold: {}, recoverThreshold:{}",
                triggerThreshold, recoverThreshold);
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

        float threshold = inHighUsage ? recoverThreshold : triggerThreshold;
        result.setHealthy(memoryStatistics.getRatio() < threshold);
        inHighUsage = !result.isHealthy();

        if (inHighUsage) {
            result.setErrorMsg(String.format("memory in high usage[%f];", memoryStatistics.getRatio()));
        }
        return result;
    }
}
