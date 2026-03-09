package com.huawei.browsergateway.adapter.impl.custom;

import com.huawei.browsergateway.adapter.AuditLogAdapter;
import com.huawei.browsergateway.adapter.dto.AuditLogInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 * 审计日志适配器 - 自定义实现
 * 适用场景：外网环境，将审计日志写入本地日志文件
 */
public class CustomAuditLogAdapter implements AuditLogAdapter {

    private static final Logger logger = LogManager.getLogger(CustomAuditLogAdapter.class);

    @Override
    public boolean writeAuditLog(AuditLogInfo auditLogInfo) {
        if (auditLogInfo == null) {
            logger.warn("AuditLogInfo is null, skip writing audit log");
            return false;
        }
        try {
            logger.info("Audit log written: type={}, operation={}", auditLogInfo.getAuditType(), auditLogInfo.getOperation());
            return true;
        } catch (Exception e) {
            logger.error("Failed to write audit log", e);
            return false;
        }
    }

}
