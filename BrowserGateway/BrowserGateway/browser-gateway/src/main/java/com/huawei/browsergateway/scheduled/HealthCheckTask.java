package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.adapter.ResourceMonitorAdapter;
import com.huawei.browsergateway.adapter.ServiceManagementAdapter;
import com.huawei.browsergateway.entity.alarm.AlarmEvent;
import com.huawei.browsergateway.entity.enums.AlarmEnum;
import com.huawei.browsergateway.service.IAlarm;
import com.huawei.browsergateway.service.healthCheck.CpuUsageCheck;
import com.huawei.browsergateway.service.healthCheck.HealthCheckResult;
import com.huawei.browsergateway.service.healthCheck.ICheckStrategy;
import com.huawei.browsergateway.service.healthCheck.MemoryUsageCheck;
import com.huawei.browsergateway.service.healthCheck.NetWorkInterfaceCheck;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 系统健康检查任务，定期执行 CPU、内存、网络接口等多项检查，
 * 汇总结果后发送/清除告警并上报健康状态到 CSE
 */
@Component
public class HealthCheckTask {

    private static final Logger log = LogManager.getLogger(HealthCheckTask.class);

    @Value("${browsergw.healthCheck.cpu-trigger-threshold}")
    private float cpuTriggerThreshold;
    @Value("${browsergw.healthCheck.cpu-recover-threshold}")
    private float cpuRecoverThreshold;
    @Value("${browsergw.healthCheck.memory-trigger-threshold}")
    private float memoryTriggerThreshold;
    @Value("${browsergw.healthCheck.memory-recover-threshold}")
    private float memoryRecoverThreshold;

    /** 检查周期（毫秒），默认 60 秒 */
    @Value("${browsergw.scheduled.health-check-period:60000}")
    private long period;

    @Autowired
    private IAlarm alarm;
    @Autowired
    private ServiceManagementAdapter serviceManagementAdapter;
    @Autowired
    private ResourceMonitorAdapter resourceMonitorAdapter;

    private List<ICheckStrategy> strategies;
    private ScheduledExecutorService scheduler;

    @PostConstruct
    private void init() {
        log.info("cpuTriggerThreshold:{}, cpuRecoverThreshold:{}, memoryTriggerThreshold:{}, memoryRecoverThreshold:{}",
                cpuTriggerThreshold, cpuRecoverThreshold, memoryTriggerThreshold, memoryRecoverThreshold);

        strategies = new ArrayList<>();
        strategies.add(new CpuUsageCheck(cpuTriggerThreshold, cpuRecoverThreshold, resourceMonitorAdapter));
        strategies.add(new MemoryUsageCheck(memoryTriggerThreshold, memoryRecoverThreshold, resourceMonitorAdapter));
        strategies.add(new NetWorkInterfaceCheck());

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkAndReport, 0, period, TimeUnit.MILLISECONDS);
        log.info("Health check task initialized, period: {}ms", period);
    }

    /** 执行所有检查策略，汇总结果后上报 */
    private void checkAndReport() {
        CheckSummary summary = runChecks();
        report(summary);
    }

    /**
     * 依次执行所有检查策略，汇总健康状态和错误信息
     *
     * @return 汇总结果
     */
    private CheckSummary runChecks() {
        boolean healthy = true;
        StringBuilder errMsg = new StringBuilder();
        for (ICheckStrategy strategy : strategies) {
            HealthCheckResult result = strategy.check();
            if (!result.isHealthy()) {
                healthy = false;
                errMsg.append(result.getErrorMsg());
            }
        }
        return new CheckSummary(healthy, errMsg.toString());
    }

    /**
     * 根据检查结果发送/清除告警，并将健康状态上报到 CSE
     *
     * @param summary 检查汇总结果
     */
    private void report(CheckSummary summary) {
        if (summary.healthy) {
            alarm.clearAlarm(new AlarmEvent(AlarmEnum.ALARM_300032, "Sub-healthy health check passed"));
        } else {
            alarm.sendAlarm(new AlarmEvent(AlarmEnum.ALARM_300032, "Sub\u2011health check failed"));
        }

        Map<String, String> healthResult = new HashMap<>();
        healthResult.put("isHealthy", Boolean.toString(summary.healthy));
        healthResult.put("checkMsg", summary.errMsg);
        serviceManagementAdapter.reportInstanceProperties(healthResult);
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            log.info("Health check task destroyed.");
        }
    }

    /** 单次检查的汇总结果值对象 */
    private static class CheckSummary {
        final boolean healthy;
        final String errMsg;

        CheckSummary(boolean healthy, String errMsg) {
            this.healthy = healthy;
            this.errMsg = errMsg;
        }
    }
}
