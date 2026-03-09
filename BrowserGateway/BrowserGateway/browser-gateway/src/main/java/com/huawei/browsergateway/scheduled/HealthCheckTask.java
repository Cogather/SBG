package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.adapter.ResourceMonitorAdapter;
import com.huawei.browsergateway.adapter.ServiceManagementAdapter;
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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 健康检查任务
 * 定期检查系统健康状态（CPU、内存、网络接口等）
 * 
 * 功能说明：
 * 1. 执行多种健康检查策略（CPU、内存、网络接口）
 * 2. 汇总检查结果
 * 3. 发送告警（如果不健康）
 * 4. 上报健康状态到服务管理适配器
 * 
 * @author BrowserGateway
 */
@Component
public class HealthCheckTask {
    
    private static final Logger log = LogManager.getLogger(HealthCheckTask.class);

    /**
     * 健康检查策略列表
     */
    private List<ICheckStrategy> strategies;
    
    /**
     * 当前健康状态
     */
    private boolean isHealthy;
    
    /**
     * 检查错误信息
     */
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
    private ServiceManagementAdapter serviceManagementAdapter;

    @Autowired
    private ResourceMonitorAdapter resourceMonitorAdapter;

    @Value("${browsergw.scheduled.health-check-period:60000}")
    private long period;
    
    private ScheduledExecutorService scheduler;

    /**
     * 初始化定时任务
     */
    @PostConstruct
    private void init() {
        log.info("cpuTriggerThreshold:{}, cpuRecoverThreshold:{}, memoryTriggerThreshold:{}, memoryRecoverThreshold:{}",
                cpuTriggerThreshold, cpuRecoverThreshold, memoryTriggerThreshold, memoryRecoverThreshold);

        // 初始化健康检查策略
        strategies = new ArrayList<>();
        strategies.add(new CpuUsageCheck(cpuTriggerThreshold, cpuRecoverThreshold, resourceMonitorAdapter));
        strategies.add(new MemoryUsageCheck(memoryTriggerThreshold, memoryRecoverThreshold, resourceMonitorAdapter));
        strategies.add(new NetWorkInterfaceCheck());

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkAndReport, 0, period, TimeUnit.MILLISECONDS);
        log.info("Health check task initialized, period: {}ms", period);
    }

    /**
     * 执行健康检查并上报结果
     */
    private void checkAndReport() {
        check();
        report();
    }

    /**
     * 执行健康检查
     */
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

    /**
     * 上报健康检查结果
     */
    private void report() {
        Map<String, String> healthResult = new HashMap<>();
        healthResult.put("isHealthy", Boolean.toString(isHealthy));
        healthResult.put("checkMsg", checkErrMsg);
        
        // 处理告警
        if (isHealthy) {
            alarm.clearAlarm(new AlarmEvent(AlarmEnum.ALARM_300032, "Sub-healthy health check passed"));
        } else {
            alarm.sendAlarm(new AlarmEvent(AlarmEnum.ALARM_300032, "Sub‑health check failed"));
        }

        // 上报健康状态到服务管理适配器
        if (serviceManagementAdapter != null) {
            serviceManagementAdapter.reportInstanceProperties(healthResult);
        }
    }

    /**
     * 销毁定时任务
     */
    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            log.info("Health check task destroyed.");
        }
    }
}
