package com.huawei.browsergateway.adapter.impl.custom;

import com.huawei.browsergateway.adapter.dto.ResourceStatistics;
import com.huawei.browsergateway.adapter.interfaces.ResourceMonitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * 资源监控适配器 - 自定义实现
 * 适用场景：外网环境，使用JMX获取系统资源
 */
@Component
public class CustomResourceMonitorAdapter implements ResourceMonitorAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomResourceMonitorAdapter.class);
    
    @Override
    public float getCpuUsage() {
        try {
            // 使用JMX获取CPU使用率
            com.sun.management.OperatingSystemMXBean osBean = 
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            double cpuUsage = osBean.getProcessCpuLoad() * 100;
            return (float) cpuUsage;
        } catch (Exception e) {
            logger.error("Failed to get CPU usage", e);
            return 0.0f;
        }
    }
    
    @Override
    public float getMemoryUsage() {
        try {
            // 使用JMX获取内存使用率
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            long used = heapUsage.getUsed();
            long max = heapUsage.getMax();
            
            if (max > 0) {
                return (float) (used * 100.0 / max);
            }
            return 0.0f;
        } catch (Exception e) {
            logger.error("Failed to get memory usage", e);
            return 0.0f;
        }
    }
    
    @Override
    public float getNetworkUsage() {
        // 外网环境暂不支持网络使用率监控
        return 0.0f;
    }
    
    @Override
    public ResourceStatistics getStatistics(String metricType) {
        ResourceStatistics stats = new ResourceStatistics();
        stats.setSuccess(true);
        stats.setTimestamp(System.currentTimeMillis());
        
        switch (metricType.toLowerCase()) {
            case "cpu":
                stats.setRatio(getCpuUsage());
                break;
            case "memory":
                stats.setRatio(getMemoryUsage());
                break;
            default:
                stats.setSuccess(false);
        }
        
        return stats;
    }
}
