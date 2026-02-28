package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.entity.alarm.AlarmEvent;
import com.huawei.browsergateway.entity.enums.AlarmEnum;
import com.huawei.browsergateway.service.IAlarm;
import com.huawei.browsergateway.service.healthCheck.CpuUsageCheck;
import com.huawei.browsergateway.service.healthCheck.ICheckStrategy;
import com.huawei.browsergateway.service.healthCheck.MemoryUsageCheck;
import com.huawei.browsergateway.service.healthCheck.NetWorkInterfaceCheck;
import com.huawei.browsergateway.service.healthCheck.HealthCheckResult;
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


@Component
public class HealthCheckTask {
    private static final Logger log = LogManager.getLogger(HealthCheckTask.class);

    private List<ICheckStrategy> strategies;
    private boolean isHealthy;
    private String checkErrMsg;

    @Value("${browsergw.healthCheck.cpu-trigger-threshold}")
    private float cpuTriggerThreshold;
    @Value("${browsergw.healthCheck.cpu-recover-threshold}")
    private float cpuRecoverThreshold;
    @Value("${browsergw.healthCheck.memory-trigger-threshold}")
    private float memoryTriggerThreshold;
    @Value("${browsergw.healthCheck.memory-recover-threshold}")
    private float memoryRecoverThreshold;

    @Autowired
    private IAlarm alarm;
    
    @Autowired
    private com.huawei.browsergateway.adapter.interfaces.ServiceManagementAdapter serviceManagementAdapter;

    @Value("${browsergw.scheduled.health-check-period:60000}")
    private long period;
    private ScheduledExecutorService scheduler;

    @PostConstruct
    private void init() {
        log.info("cpuTriggerThreshold:{}, cpuRecoverThreshold:{}, memoryTriggerThreshold:{}, memoryRecoverThreshold:{}",
                cpuTriggerThreshold, cpuRecoverThreshold, memoryTriggerThreshold, memoryRecoverThreshold);

        strategies = new ArrayList<>();
        strategies.add(new CpuUsageCheck(cpuTriggerThreshold, cpuRecoverThreshold));
        strategies.add(new MemoryUsageCheck(memoryTriggerThreshold, memoryRecoverThreshold));
        strategies.add(new NetWorkInterfaceCheck());

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkAndReport, 0, period, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void checkAndReport() {
        check();
        report();
    }

    private void check() {
        boolean isHealthy = true;
        StringBuilder errMsg = new StringBuilder();
        for (ICheckStrategy strategy : strategies) {
            HealthCheckResult check = strategy.check();
            if (!check.isHealthy()) {
                isHealthy = false;
                errMsg.append(check.getErrorMsg());
            }
        }
        this.isHealthy = isHealthy;
        this.checkErrMsg = errMsg.toString();
    }

    private void report() {
        Map<String, String> healthResult = new HashMap<>();
        healthResult.put("isHealthy", Boolean.toString(isHealthy));
        healthResult.put("checkMsg", checkErrMsg);
        if (isHealthy) {
            alarm.clearAlarm(AlarmEnum.ALARM_300032.getAlarmId());
        } else {
            alarm.sendAlarm(new AlarmEvent(AlarmEnum.ALARM_300032, "Sub‑health check failed" ));
        }

        if (serviceManagementAdapter != null) {
            serviceManagementAdapter.reportInstanceProperties(healthResult);
        }
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}