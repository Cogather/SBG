package com.huawei.browsergateway.util;

import com.huawei.browsergateway.adapter.AuditLogAdapter;
import com.huawei.browsergateway.adapter.dto.AuditLevel;
import com.huawei.browsergateway.adapter.dto.AuditResult;
import com.huawei.browsergateway.adapter.dto.AuditType;
import com.huawei.browsergateway.adapter.dto.OperateType;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 审计日志工具类，封装审计日志写入逻辑 */
@Component
public class AuditLogUtil {

    private static final Logger log = LogManager.getLogger(AuditLogUtil.class);
    private static final String APP_NAME = "browsergw";

    /** Spring 注入后的单例，供静态方法使用 */
    private static AuditLogUtil instance;

    private final AuditLogAdapter auditLogAdapter;

    @Autowired
    public AuditLogUtil(AuditLogAdapter auditLogAdapter) {
        this.auditLogAdapter = auditLogAdapter;
        AuditLogUtil.instance = this;
    }

    /** 获取实例（用于非 Spring 环境） */
    public static AuditLogUtil getInstance() {
        return instance;
    }

    /**
     * 审计日志信息（链式 Builder 风格）
     *
     * <p>level 可选值：WARNING / MINOR / RISK<br>
     * result 可选值：SUCCESSFUL / FAILURE / PARTIAL_SUCCESS
     */
    @Data
    @Accessors(chain = true)
    public static class AuditLogInfo {
        /** 操作名称 */
        private String operation;
        /** 日志级别：WARNING / MINOR / RISK */
        private String level;
        /** 操作用户 */
        private String userName;
        /** 时间戳 */
        private String dateTime;
        /** 操作来源 */
        private String appName;
        /** 客户端 IP */
        private String terminal;
        /** 操作对象 */
        private String serviceName;
        /** 操作结果：SUCCESSFUL / FAILURE / PARTIAL_SUCCESS */
        private String result;
        /** 详细信息（英文，最多 800 字符） */
        private String detail;
        /** 详细信息（中文，最多 800 字符） */
        private String detailZh;
    }

    /**
     * 写入审计日志（静态入口，保持向后兼容）
     *
     * @param auditType    审计类型
     * @param auditLogInfo 审计日志信息
     * @param level        日志级别
     * @param operateType  操作类型
     * @param result       操作结果
     */
    public static void writeAuditLog(AuditType auditType, AuditLogInfo auditLogInfo,
            AuditLevel level, OperateType operateType, AuditResult result) {
        if (instance != null) {
            instance.doWriteAuditLog(auditType, auditLogInfo, level, operateType, result);
        } else {
            log.warn("AuditLogUtil instance not initialized, skipping audit log write");
        }
    }

    /** 实例方法：执行实际写入 */
    private void doWriteAuditLog(AuditType auditType, AuditLogInfo auditLogInfo,
            AuditLevel level, OperateType operateType, AuditResult result) {
        try {
            com.huawei.browsergateway.adapter.dto.AuditLogInfo adapterInfo =
                    buildAdapterInfo(auditType, auditLogInfo, level, operateType, result);
            boolean success = auditLogAdapter.writeAuditLog(adapterInfo);
            if (!success) {
                log.warn("Failed to write audit log for operation: {}", auditLogInfo.getOperation());
            }
        } catch (Exception e) {
            log.error("Error writing audit log, detail: {}", auditLogInfo.getDetail(), e);
        }
    }

    /** 将本地 AuditLogInfo 转换为适配器所需的 DTO */
    private com.huawei.browsergateway.adapter.dto.AuditLogInfo buildAdapterInfo(
            AuditType auditType, AuditLogInfo info,
            AuditLevel level, OperateType operateType, AuditResult result) {
        com.huawei.browsergateway.adapter.dto.AuditLogInfo dto =
                new com.huawei.browsergateway.adapter.dto.AuditLogInfo();
        dto.setOperation(info.getOperation());
        dto.setLevel(String.valueOf(level != null ? level.getCodeLevel() : AuditLevel.MINOR.getCodeLevel()));
        dto.setUserName(info.getUserName());
        dto.setDateTime(String.valueOf(System.currentTimeMillis()));
        dto.setAppName(APP_NAME);
        dto.setTerminal(info.getTerminal());
        dto.setServiceName(APP_NAME);
        dto.setResult(String.valueOf(result != null ? result.getCodeStatus() : AuditResult.SUCCESSFUL.getCodeStatus()));
        dto.setDetail(info.getDetail());
        dto.setDetailZh(info.getDetailZh());
        dto.setAuditType(auditType.name());
        if (!AuditType.SECURITY.equals(auditType)) {
            dto.setOperateType(String.valueOf(
                    operateType != null ? operateType.getCodeType() : OperateType.UPHOLD.getCodeType()));
        }
        return dto;
    }
}
