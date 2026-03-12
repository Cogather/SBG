package com.huawei.browsergateway.service.healthCheck;

import lombok.Data;

/**
 * 单项健康检查的结果，包含检查项名称、是否健康及错误信息
 */
@Data
public class HealthCheckResult {

    /** 检查项名称 */
    private String checkItem;

    /** 是否健康 */
    private boolean isHealthy;

    /** 异常描述，健康时为空 */
    private String errorMsg;
}
