package com.huawei.browsergateway.service.healthCheck;

import lombok.Data;

@Data
public class HealthCheckResult {
    private String checkItem;   // 检查项名称
    private boolean isHealthy;  // 是否健康
    private String errorMsg;    // 异常信息
}
