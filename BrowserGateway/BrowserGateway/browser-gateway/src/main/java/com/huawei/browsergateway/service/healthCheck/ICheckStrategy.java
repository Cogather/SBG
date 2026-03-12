package com.huawei.browsergateway.service.healthCheck;

/**
 * 健康检查策略接口，所有检查项均实现此接口
 */
public interface ICheckStrategy {

    /**
     * 执行一次健康检查，返回检查结果
     */
    HealthCheckResult check();
}
