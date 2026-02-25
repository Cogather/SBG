package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.adapter.interfaces.ServiceManagementAdapter;
import com.huawei.browsergateway.service.IChromeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 容量上报任务
 * 上报浏览器实例使用情况到CSE
 */
@Component
public class CapacityReportTask {
    
    private static final Logger log = LoggerFactory.getLogger(CapacityReportTask.class);
    
    @Autowired
    private IChromeSet chromeSet;
    
    @Autowired
    private ServiceManagementAdapter serviceManagementAdapter;
    
    // 服务容量上限
    @Value("${browsergw.report.cap:300}")
    private int capacityCap;
    
    // 容量上报周期（默认5分钟）
    @Value("${browsergw.scheduled.capacity-report-period:300000}")
    private long capacityReportPeriod;
    
    /**
     * 上报容量信息
     * 默认每5分钟执行一次
     */
    @Scheduled(fixedDelayString = "${browsergw.scheduled.capacity-report-period:300000}")
    public void reportCapacity() {
        log.debug("开始执行容量上报任务");
        
        try {
            // 获取当前使用的实例数
            int usedInstances = chromeSet.getAllUser().size();
            
            // 计算使用率
            float usageRate = capacityCap > 0 ? (float) usedInstances / capacityCap * 100 : 0;
            
            // 构建上报属性
            Map<String, String> properties = new HashMap<>();
            properties.put("usedInstances", String.valueOf(usedInstances));
            properties.put("capacityCap", String.valueOf(capacityCap));
            properties.put("usageRate", String.format("%.2f", usageRate));
            properties.put("capacityReportTime", String.valueOf(System.currentTimeMillis()));
            
            // 上报到CSE
            boolean success = serviceManagementAdapter.reportInstanceProperties(properties);
            
            if (success) {
                log.debug("容量上报成功: usedInstances={}, capacityCap={}, usageRate={}%", 
                    usedInstances, capacityCap, usageRate);
            } else {
                log.warn("容量上报失败: usedInstances={}, capacityCap={}", usedInstances, capacityCap);
            }
            
        } catch (Exception e) {
            log.error("容量上报任务执行异常", e);
        }
    }
}
