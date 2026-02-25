package com.huawei.browsergateway.service.healthCheck.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 健康检查结果
 */
@Data
@Builder
public class HealthCheckResult {
    
    /**
     * 指标名称
     */
    private String metricName;
    
    /**
     * 指标值
     */
    private float value;
    
    /**
     * 是否健康
     */
    private boolean healthy;
    
    /**
     * 检查消息
     */
    private String message;
    
    /**
     * 时间戳
     */
    private long timestamp;
    
    /**
     * 阈值（触发告警的阈值）
     */
    private float threshold;
    
    /**
     * 恢复阈值（告警恢复的阈值）
     */
    private float recoverThreshold;
}
