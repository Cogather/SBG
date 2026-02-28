package com.huawei.browsergateway.adapter.impl.custom;

import com.huawei.browsergateway.adapter.dto.ResourceStatistics;
import com.huawei.browsergateway.adapter.interfaces.ResourceMonitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;

/**
 * 资源监控适配器 - 自定义实现
 */
@Component("customResourceMonitorAdapter")
@ConditionalOnProperty(name = "adapter.provider.type", havingValue = "CUSTOM")
public class CustomResourceMonitorAdapter implements ResourceMonitorAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomResourceMonitorAdapter.class);
    
    @Override
    public float getCpuUsage() {
        ResourceStatistics stats = getStatistics("cpu");
        return stats != null ? stats.getRatio() : 0.0f;
    }
    
    @Override
    public float getMemoryUsage() {
        ResourceStatistics stats = getStatistics("memory");
        return stats != null ? stats.getRatio() : 0.0f;
    }
    
    @Override
    public float getNetworkUsage() {
        // 外网环境暂不支持网络监控
        return 0.0f;
    }
    
    @Override
    public ResourceStatistics getStatistics(String metricType) {
        ResourceStatistics stats = new ResourceStatistics();
        stats.setTimestamp(System.currentTimeMillis());
        
        try {
            if ("cpu".equals(metricType)) {
                OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
                if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                    com.sun.management.OperatingSystemMXBean sunOsBean = 
                            (com.sun.management.OperatingSystemMXBean) osBean;
                    double cpuLoad = sunOsBean.getProcessCpuLoad();
                    if (cpuLoad >= 0) {
                        stats.setSuccess(true);
                        stats.setRatio((float) (cpuLoad * 100));
                    } else {
                        stats.setSuccess(false);
                        stats.setRatio(0.0f);
                    }
                } else {
                    // 使用系统负载平均值
                    double loadAverage = osBean.getSystemLoadAverage();
                    if (loadAverage >= 0) {
                        stats.setSuccess(true);
                        stats.setRatio((float) (loadAverage * 10)); // 简单转换
                    } else {
                        stats.setSuccess(false);
                        stats.setRatio(0.0f);
                    }
                }
            } else if ("memory".equals(metricType)) {
                MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
                MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
                long used = heapUsage.getUsed();
                long max = heapUsage.getMax();
                
                if (max > 0) {
                    stats.setSuccess(true);
                    stats.setRatio((float) (used * 100.0 / max));
                    stats.setAvailable(max - used);
                    stats.setCapacity(max);
                } else {
                    stats.setSuccess(false);
                    stats.setRatio(0.0f);
                }
            } else {
                stats.setSuccess(false);
                stats.setRatio(0.0f);
            }
        } catch (Exception e) {
            logger.error("Failed to get resource statistics", e);
            stats.setSuccess(false);
            stats.setRatio(0.0f);
        }
        
        return stats;
    }
}
