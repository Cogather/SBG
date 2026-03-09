package com.huawei.browsergateway.adapter;

import com.huawei.browsergateway.adapter.dto.AuditLogInfo;

/**
 * 审计日志适配器接口
 * 职责：处理审计日志的写入
 */
public interface AuditLogAdapter {

    /**
     * 写入审计日志
     * @param auditLogInfo 审计日志信息
     * @return 写入是否成功
     */
    boolean writeAuditLog(AuditLogInfo auditLogInfo);
}
