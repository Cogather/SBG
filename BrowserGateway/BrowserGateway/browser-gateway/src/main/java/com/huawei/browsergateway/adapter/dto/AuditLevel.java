package com.huawei.browsergateway.adapter.dto;

/**
 * 审计日志级别枚举
 */
public enum AuditLevel {
    WARNING(0),
    MINOR(1),
    RISK(2),
    AUTOQUERY(3),
    QUERY(4);

    private final int codeLevel;

    AuditLevel(int codeLevel) {
        this.codeLevel = codeLevel;
    }

    public int getCodeLevel() {return this.codeLevel;}
}
