package com.huawei.browsergateway.adapter.interfaces;

import com.huawei.browsergateway.adapter.dto.ResourceStatistics;

/**
 * 资源监控适配器接口
 * 职责：CPU、内存等系统资源监控
 */
public interface ResourceMonitorAdapter {
    
    /**
     * 获取CPU使用率
     * @return CPU使用率百分比（0-100）
     */
    float getCpuUsage();
    
    /**
     * 获取内存使用率
     * @return 内存使用率百分比（0-100）
     */
    float getMemoryUsage();
    
    /**
     * 获取网络带宽使用率
     * @return 带宽使用率百分比（0-100）
     */
    float getNetworkUsage();
    
    /**
     * 获取资源统计信息
     * @param metricType 指标类型（cpu、memory、network）
     * @return 资源统计信息
     */
    ResourceStatistics getStatistics(String metricType);
}
