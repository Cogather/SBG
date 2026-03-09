package com.huawei.browsergateway.util;

import com.huawei.browsergateway.adapter.AuditLogAdapter;
import com.huawei.browsergateway.adapter.dto.AuditLevel;
import com.huawei.browsergateway.adapter.dto.AuditResult;
import com.huawei.browsergateway.adapter.dto.AuditType;
import com.huawei.browsergateway.adapter.dto.OperateType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuditLogUtil {
    private static final Logger log = LogManager.getLogger(AuditLogUtil.class);

    private static AuditLogUtil instance;

    private final AuditLogAdapter auditLogAdapter;

    @Autowired
    public AuditLogUtil(AuditLogAdapter auditLogAdapter) {
        this.auditLogAdapter = auditLogAdapter;
        AuditLogUtil.instance = this;
    }

    /**
     * 获取实例（用于非Spring环境）
     */
    public static AuditLogUtil getInstance() {
        return instance;
    }

    /**
     * 审计日志信息（保留用于向后兼容）
     */
    public static class AuditLogInfo {
        /**
         * 操作名称
         */
        private String operation;

        /**
         * 日志级别。可以是如下值之一：
         * WARNING：提示
         * MINOR：一般
         * RISK：危险
         */
        private String level;

        /**
         * 操作用户
         */
        private String userName;

        /**
         * 时间戳
         */
        private String dateTime;

        /**
         * 操作来源
         */
        private String appName;

        /**
         * 发起操作的客户端IP地址，可从HTTP Header中获取
         */
        private String terminal;

        /**
         * 操作对象
         */
        private String serviceName;

        /**
         * 操作结果。可以是如下值之一：
         * SUCCESSFUL：成功
         * FAILURE：失败
         * PARTIAL_SUCCESS：部分成功
         */
        private String result;

        /**
         * 详细信息，最多支持800个字符
         */
        private String detail;

        /**
         * 详细信息(中文)，最多支持800个字符
         */
        private String detailZh;

        public String getOperation() {
            return operation;
        }

        public AuditLogInfo setOperation(String operation) {
            this.operation = operation;
            return this;
        }

        public String getLevel() {
            return level;
        }

        public AuditLogInfo setLevel(String level) {
            this.level = level;
            return this;
        }

        public String getUserName() {
            return userName;
        }

        public AuditLogInfo setUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public String getDateTime() {
            return dateTime;
        }

        public AuditLogInfo setDateTime(String dateTime) {
            this.dateTime = dateTime;
            return this;
        }

        public String getAppName() {
            return appName;
        }

        public AuditLogInfo setAppName(String appName) {
            this.appName = appName;
            return this;
        }

        public String getTerminal() {
            return terminal;
        }

        public AuditLogInfo setTerminal(String terminal) {
            this.terminal = terminal;
            return this;
        }

        public String getServiceName() {
            return serviceName;
        }

        public AuditLogInfo setServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public String getResult() {
            return result;
        }

        public AuditLogInfo setResult(String result) {
            this.result = result;
            return this;
        }

        public String getDetail() {
            return detail;
        }

        public AuditLogInfo setDetail(String detail) {
            this.detail = detail;
            return this;
        }

        public String getDetailZh() {
            return detailZh;
        }

        public AuditLogInfo setDetailZh(String detailZh) {
            this.detailZh = detailZh;
            return this;
        }
    }

    /**
     * 写入审计日志（静态方法，保持向后兼容）
     * @param auditType 审计日志类型
     * @param auditLogInfo 审计日志信息
     * @param level 日志级别
     * @param operateType 操作类型
     * @param result 操作结果
     */
    public static void writeAuditLog(
            AuditType auditType, AuditLogInfo auditLogInfo,
            AuditLevel level, OperateType operateType, AuditResult result) {
        if (instance != null) {
            instance.writeAuditLogInstance(auditType, auditLogInfo, level, operateType, result);
        } else {
            log.warn("AuditLogUtil instance not initialized, skipping audit log write");
        }
    }

    /**
     * 写入审计日志（实例方法）
     * @param auditType 审计日志类型
     * @param auditLogInfo 审计日志信息
     * @param level 日志级别
     * @param operateType 操作类型
     * @param result 操作结果
     */
    private void writeAuditLogInstance(
            AuditType auditType, AuditLogInfo auditLogInfo,
            AuditLevel level, OperateType operateType, AuditResult result) {
        try {
            com.huawei.browsergateway.adapter.dto.AuditLogInfo adapterAuditLogInfo = buildAdapterAuditLogInfo(
                    auditType, auditLogInfo, level, operateType, result);
            boolean success = auditLogAdapter.writeAuditLog(adapterAuditLogInfo);
            if (!success) {
                log.warn("Failed to write audit log for operation: {}", auditLogInfo.getOperation());
            }
        } catch (Exception e) {
            log.error("Error writing audit log, detail: {}, error: {}",
                    auditLogInfo.getDetail(), e.getMessage(), e);
        }
    }

    /**
     * 构建适配器审计日志信息
     */
    private com.huawei.browsergateway.adapter.dto.AuditLogInfo buildAdapterAuditLogInfo(
            AuditType auditType, AuditLogInfo auditLogInfo,
            AuditLevel level, OperateType operateType, AuditResult result) {
        com.huawei.browsergateway.adapter.dto.AuditLogInfo adapterInfo = new com.huawei.browsergateway.adapter.dto.AuditLogInfo();
        adapterInfo.setOperation(auditLogInfo.getOperation());
        adapterInfo.setLevel(String.valueOf((level != null) ? level.getCodeLevel() : AuditLevel.MINOR.getCodeLevel()));
        adapterInfo.setUserName(auditLogInfo.getUserName());
        adapterInfo.setDateTime(String.valueOf(System.currentTimeMillis()));
        adapterInfo.setAppName("browsergw");
        adapterInfo.setTerminal(auditLogInfo.getTerminal());
        adapterInfo.setServiceName("browsergw");
        adapterInfo.setResult(String.valueOf((result != null) ? result.getCodeStatus() : AuditResult.SUCCESSFUL.getCodeStatus()));
        adapterInfo.setDetail(auditLogInfo.getDetail());
        adapterInfo.setDetailZh(auditLogInfo.getDetailZh());
        adapterInfo.setAuditType(auditType.name());
        if (!AuditType.SECURITY.equals(auditType)) {
            adapterInfo.setOperateType(String.valueOf((operateType != null) ? operateType.getCodeType() : OperateType.UPHOLD.getCodeType()));
        }
        return adapterInfo;
    }
}