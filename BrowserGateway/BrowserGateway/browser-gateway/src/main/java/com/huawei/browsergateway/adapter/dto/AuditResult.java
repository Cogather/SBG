package com.huawei.browsergateway.adapter.dto;

/**
 * 审计日志结果枚举
 */
public enum AuditResult {
    SUCCESSFUL(0),
    FAILURE(1),
    PARTIAL_SUCCESS(2);

    private final int codeStatus;

    AuditResult(int codeStatus) {
        this.codeStatus = codeStatus;
    }
    public int getCodeStatus() {
        return codeStatus;
    }
}
