package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.adapter.interfaces.ServiceManagementAdapter;
import com.huawei.browsergateway.common.enums.ErrorCodeEnum;
import com.huawei.browsergateway.entity.alarm.AlarmEvent;
import com.huawei.browsergateway.service.IAlarm;
import com.huawei.browsergateway.service.healthCheck.CpuUsageCheck;
import com.huawei.browsergateway.service.healthCheck.ICheckStrategy;
import com.huawei.browsergateway.service.healthCheck.MemoryUsageCheck;
import com.huawei.browsergateway.service.healthCheck.NetworkUsageCheck;
import com.huawei.browsergateway.service.healthCheck.dto.HealthCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康检查任务
 * 检查系统健康状态，包括CPU、内存、网络等
 * 根据检查结果触发告警并上报健康状态到CSE
 */
@Component
public class HealthCheckTask {
    
    private static final Logger log = LoggerFactory.getLogger(HealthCheckTask.class);
    
    @Autowired
    private CpuUsageCheck cpuUsageCheck;
    
    @Autowired
    private MemoryUsageCheck memoryUsageCheck;
    
    @Autowired
    private NetworkUsageCheck networkUsageCheck;
    
    @Autowired
    private IAlarm alarmService;
    
    @Autowired
    private ServiceManagementAdapter serviceManagementAdapter;
    
    // 健康检查周期（默认1分钟）
    @Value("${browsergw.scheduled.health-check-period:60000}")
    private long healthCheckPeriod;
    
    // 记录上次告警状态，用于判断是否需要恢复告警
    private final Map<String, Boolean> lastAlarmStatus = new HashMap<>();
    
    /**
     * 执行健康检查并上报
     * 默认每1分钟执行一次
     */
    @Scheduled(fixedDelayString = "${browsergw.scheduled.health-check-period:60000}")
    public void checkAndReport() {
        log.debug("开始执行健康检查任务");
        
        try {
            // 执行各项健康检查
            List<HealthCheckResult> results = new ArrayList<>();
            
            // CPU检查
            HealthCheckResult cpuResult = cpuUsageCheck.check();
            results.add(cpuResult);
            handleHealthCheckResult(cpuResult, ErrorCodeEnum.CPU_USAGE_HIGH, "CPU");
            
            // 内存检查
            HealthCheckResult memoryResult = memoryUsageCheck.check();
            results.add(memoryResult);
            handleHealthCheckResult(memoryResult, ErrorCodeEnum.MEMORY_USAGE_HIGH, "内存");
            
            // 网络检查
            HealthCheckResult networkResult = networkUsageCheck.check();
            results.add(networkResult);
            handleHealthCheckResult(networkResult, ErrorCodeEnum.NETWORK_USAGE_HIGH, "网络");
            
            // 上报健康状态到CSE
            reportHealthStatus(results);
            
            log.debug("健康检查任务执行完成");
            
        } catch (Exception e) {
            log.error("健康检查任务执行异常", e);
            
            // 健康检查失败时发送告警
            AlarmEvent alarmEvent = new AlarmEvent();
            alarmEvent.setAlarmCodeEnum(ErrorCodeEnum.HEALTH_REPORT_FAILURE);
            alarmEvent.setEventMessage("健康检查任务执行失败: " + e.getMessage());
            alarmService.sendAlarm(alarmEvent);
        }
    }
    
    /**
     * 处理健康检查结果
     * 
     * @param result 健康检查结果
     * @param alarmCode 告警码
     * @param metricName 指标名称
     */
    private void handleHealthCheckResult(HealthCheckResult result, ErrorCodeEnum alarmCode, String metricName) {
        String alarmKey = metricName + "_" + alarmCode.getCode();
        boolean currentStatus = !result.isHealthy();
        boolean lastStatus = lastAlarmStatus.getOrDefault(alarmKey, false);
        
        // 判断是否需要触发告警
        if (currentStatus && !lastStatus) {
            // 状态从不健康变为不健康，触发告警
            log.warn("{}健康检查不通过，触发告警: {}", metricName, result.getMessage());
            
            AlarmEvent alarmEvent = new AlarmEvent();
            alarmEvent.setAlarmCodeEnum(alarmCode);
            alarmEvent.setEventMessage(result.getMessage());
            alarmService.sendAlarm(alarmEvent);
            
            lastAlarmStatus.put(alarmKey, true);
            
        } else if (!currentStatus && lastStatus) {
            // 状态从健康变为健康，清除告警
            log.info("{}健康检查恢复正常，清除告警: {}", metricName, result.getMessage());
            
            alarmService.clearAlarm(String.valueOf(alarmCode.getCode()));
            lastAlarmStatus.put(alarmKey, false);
        }
    }
    
    /**
     * 上报健康状态到CSE
     * 
     * @param results 健康检查结果列表
     */
    private void reportHealthStatus(List<HealthCheckResult> results) {
        try {
            Map<String, String> properties = new HashMap<>();
            
            // 构建健康状态属性
            for (HealthCheckResult result : results) {
                String metricName = result.getMetricName();
                properties.put(metricName, String.valueOf(result.getValue()));
                properties.put(metricName + "_healthy", String.valueOf(result.isHealthy()));
                properties.put(metricName + "_timestamp", String.valueOf(result.getTimestamp()));
            }
            
            // 计算整体健康状态
            boolean overallHealthy = results.stream().allMatch(HealthCheckResult::isHealthy);
            properties.put("overall_healthy", String.valueOf(overallHealthy));
            properties.put("health_check_time", String.valueOf(System.currentTimeMillis()));
            
            // 上报到CSE
            boolean success = serviceManagementAdapter.reportInstanceProperties(properties);
            
            if (success) {
                log.debug("健康状态上报成功");
            } else {
                log.warn("健康状态上报失败");
            }
            
        } catch (Exception e) {
            log.error("上报健康状态异常", e);
        }
    }
}
