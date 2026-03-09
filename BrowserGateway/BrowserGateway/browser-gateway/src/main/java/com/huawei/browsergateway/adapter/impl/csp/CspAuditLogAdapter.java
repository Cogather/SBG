package com.huawei.browsergateway.adapter.impl.csp;

import com.huawei.browsergateway.adapter.AuditLogAdapter;
import com.huawei.browsergateway.adapter.dto.AuditLogInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;

/**
 * 审计日志适配器 - CSP SDK实现
 * 通过CspRestTemplateBuilder调用CSP审计日志REST接口
 */
@Component("cspAuditLogAdapter")
public class CspAuditLogAdapter implements AuditLogAdapter {

    private static final Logger logger = LogManager.getLogger(CspAuditLogAdapter.class);

    private static final String OPER_LOG_PATH = "cse://AuditLog/plat/audit/v1/logs";
    private static final String SECURITY_LOG_PATH = "cse://AuditLog/plat/audit/v1/seculogs";

    @Override
    public boolean writeAuditLog(AuditLogInfo auditLogInfo) {
        if (auditLogInfo == null) {
            logger.warn("AuditLogInfo is null, skip writing audit log");
            return false;
        }

        try {
            String url = resolveLogPath(auditLogInfo.getAuditType());
            RestTemplate restTemplate = createCspRestTemplate();
            if (restTemplate == null) {
                logger.error("Failed to create CspRestTemplate");
                return false;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AuditLogInfo> requestEntity = new HttpEntity<>(auditLogInfo, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            boolean success = response.getStatusCode().is2xxSuccessful();
            if (!success) {
                logger.error("Failed to write audit log, status: {}", response.getStatusCodeValue());
            }
            return success;
        } catch (Exception e) {
            logger.error("Failed to write audit log", e);
            return false;
        }
    }

    private String resolveLogPath(String auditType) {
        if ("SECURITY".equalsIgnoreCase(auditType)) {
            return SECURITY_LOG_PATH;
        }
        return OPER_LOG_PATH;
    }

    private RestTemplate createCspRestTemplate() {
        try {
            Class<?> builderClass = Class.forName("com.huawei.csp.jsf.api.CspRestTemplateBuilder");
            Method create = builderClass.getMethod("create");
            return (RestTemplate) create.invoke(null);
        } catch (Exception e) {
            logger.error("Failed to create CspRestTemplate via reflection", e);
            return null;
        }
    }
}
