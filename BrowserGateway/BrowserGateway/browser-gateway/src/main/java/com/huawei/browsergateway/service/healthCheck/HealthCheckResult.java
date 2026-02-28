package com.huawei.browsergateway.service.healthCheck;

import lombok.Data;

@Data
public class HealthCheckResult {
    private boolean healthy;
    private String errorMsg;
    private String checkItem;
}
