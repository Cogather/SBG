package com.huawei.browsergateway.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


public class AuditLogUtil {
    private static Logger log = LogManager.getLogger(AuditLogUtil.class);

    private static final String OPER_LOG_PATH = "cse://AuditLog/plat/audit/v1/logs";

    private static final String SECURITY_LOG_PATH = "cse://AuditLog/plat/audit/v1/seculogs";

    private static final String APPNAME = "browsergw";

    private static final String SERVICENAME = "browsergw";


    public enum AuditType {
        OPERATION,
        SECURITY
    }

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

    public enum AuditLevel {
        /**
         * 提示
         */
        WARNING(0),
        /**
         * 一般
         */
        MINOR(1),
        /**
         * 危险
         */
        RISK(2),
        /**
         * 自动查询
         */
        AUTOQUERY(3),
        /**
         * 手动查询
         */
        QUERY(4);

        int codeLevel;

        AuditLevel(int code) {
            codeLevel = code;
        }
    }

    /**
     * 操作类型
     *
     * @since 2022/7/28
     */
    public enum OperateType {
        /**
         * 查询
         */
        GET(0),
        /**
         * 提示
         */
        ADD(1),
        /**
         * 一般
         */
        MOD(2),
        /**
         * 危险
         */
        DELETE(3),
        /**
         * 自动查询
         */
        DOWNLOAD(4),
        /**
         * 手动查询
         */
        UPLOAD(5),
        /**
         * 手动查询
         */
        UPHOLD(6);

        int codeType;

        OperateType(int code) {
            codeType = code;
        }
    }

    /**
     * 鉴权操作结果
     *
     * @since 2019/3/20
     */
    public enum AuditResult {
        /**
         * 成功
         */
        SUCCESSFUL(0),
        /**
         * 失败
         */
        FAILURE(1),
        /**
         * 部分成功
         */
        PARTIAL_SUCCESS(2);

        int codeStaus;

        AuditResult(int code) {
            codeStaus = code;
        }
    }

    public static void writeAuditLog(
            AuditType auditType, AuditLogInfo auditLogInfo, AuditLevel level, OperateType operateType, AuditResult result) {
        // 默认为操作日志
        String path = OPER_LOG_PATH;
        // 安全日志场景
        if (AuditType.SECURITY.equals(auditType)) {
            path = SECURITY_LOG_PATH;
        }

        // 填充日志消息体
        JSONObject body = new JSONObject();
        body.put("operation", auditLogInfo.getOperation());
        body.put("level", (level != null) ? level.codeLevel : AuditLevel.MINOR);
        body.put("userName", auditLogInfo.getUserName());
        body.put("dateTime", System.currentTimeMillis());
        body.put("appId", 0);
        body.put("appName", APPNAME);
        body.put("terminal", auditLogInfo.getTerminal());
        body.put("serviceName", SERVICENAME);
        body.put("result", (result != null) ? result.codeStaus : AuditResult.SUCCESSFUL);
        body.put("detail", auditLogInfo.getDetail());
        body.put("detail_zh", auditLogInfo.getDetailZh());
        if (!AuditType.SECURITY.equals(auditType)) {
            body.put("operateType", (operateType != null) ? operateType.codeType : OperateType.UPHOLD);
        }

        // 如果使用 cse:// 协议但 CSP SDK 不可用，则跳过审计日志记录
        if (path.startsWith("cse://") && !isCspSdkAvailable()) {
            log.debug("CSP SDK not available, skipping audit log with cse:// protocol: {}", path);
            return;
        }

        try {
            log.info("report audit log, {}", body.toString());
            RestTemplate restTemplate = createRestTemplate();
            HttpEntity<String> requestEntity = new HttpEntity<>(body.toString());
            ResponseEntity<Integer> response =
                    restTemplate.exchange(path, HttpMethod.POST, requestEntity, Integer.class);
            int statusCode = response.getStatusCodeValue();
            if (HttpStatus.OK.value() != statusCode) {
                log.error("responsestatus = {} ,sn = {}", statusCode, response.getBody());
                return;
            }
            log.info("reported audit log");
        } catch (Throwable e) {
            log.error("rest connect failed, detail: {} ,Throwable:{}", auditLogInfo.getDetail(), e.getMessage());
        }
    }
    
    /**
     * 检查CSP SDK是否可用
     */
    private static boolean isCspSdkAvailable() {
        try {
            Class.forName("com.huawei.csp.jsf.api.CspRestTemplateBuilder");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 创建RestTemplate，优先使用CSP SDK的CspRestTemplateBuilder，否则使用标准RestTemplate
     */
    private static RestTemplate createRestTemplate() {
        try {
            // 尝试使用CSP SDK的CspRestTemplateBuilder
            Class<?> builderClass = Class.forName("com.huawei.csp.jsf.api.CspRestTemplateBuilder");
            java.lang.reflect.Method createMethod = builderClass.getMethod("create");
            return (RestTemplate) createMethod.invoke(null);
        } catch (Exception e) {
            // 如果CSP SDK不可用，使用标准RestTemplate
            log.debug("CspRestTemplateBuilder not available, using standard RestTemplate", e);
            return new RestTemplate();
        }
    }
}