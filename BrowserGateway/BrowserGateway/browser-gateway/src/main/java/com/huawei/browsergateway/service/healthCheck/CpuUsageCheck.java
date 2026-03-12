package com.huawei.browsergateway.service.healthCheck;

import com.huawei.browsergateway.adapter.ResourceMonitorAdapter;
import com.huawei.browsergateway.adapter.dto.ResourceStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CPU 使用率健康检查策略。
 * 采用滞回阈值：超过 triggerThreshold 触发告警，低于 recoverThreshold 才恢复正常。
 */
public class CpuUsageCheck implements ICheckStrategy {

    private static final Logger log = LoggerFactory.getLogger(CpuUsageCheck.class);

    private final float triggerThreshold;
    private final float recoverThreshold;
    private final ResourceMonitorAdapter resourceMonitorAdapter;

    /** 当前是否处于高负载状态（用于滞回判断） */
    private boolean inHighUsage = false;

    public CpuUsageCheck(float triggerThreshold, float recoverThreshold,
                         ResourceMonitorAdapter resourceMonitorAdapter) {
        this.triggerThreshold = triggerThreshold;
        this.recoverThreshold = recoverThreshold;
        this.resourceMonitorAdapter = resourceMonitorAdapter;
        log.info("start to check cpu usage, triggerThreshold: {}, recoverThreshold:{}",
                triggerThreshold, recoverThreshold);
    }

    @Override
    public HealthCheckResult check() {
        HealthCheckResult result = new HealthCheckResult();
        result.setCheckItem("CpuUsageCheck");
        result.setHealthy(true);

        ResourceStatistics cpuStatistics = resourceMonitorAdapter.getStatistics("cpu");
        if (!cpuStatistics.isSuccess()) {
            return result;
        }

        float threshold = inHighUsage ? recoverThreshold : triggerThreshold;
        result.setHealthy(cpuStatistics.getRatio() < threshold);
        inHighUsage = !result.isHealthy();

        if (inHighUsage) {
            result.setErrorMsg(String.format("cpu in high usage[%f];", cpuStatistics.getRatio()));
        }
        return result;
    }
}
