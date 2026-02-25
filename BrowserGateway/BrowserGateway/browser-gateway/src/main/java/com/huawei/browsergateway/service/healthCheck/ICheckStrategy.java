package com.huawei.browsergateway.service.healthCheck;

import com.huawei.browsergateway.service.healthCheck.dto.HealthCheckResult;

/**
 * 健康检查策略接口
 * 定义各种健康检查策略的统一接口
 */
public interface ICheckStrategy {
    
    /**
     * 执行健康检查
     * 
     * @return 健康检查结果
     */
    HealthCheckResult check();
    
    /**
     * 获取检查策略名称
     * 
     * @return 策略名称
     */
    String getStrategyName();
}
