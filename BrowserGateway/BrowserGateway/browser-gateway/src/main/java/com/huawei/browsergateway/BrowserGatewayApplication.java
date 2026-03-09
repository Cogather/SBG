package com.huawei.browsergateway;

import com.alibaba.fastjson2.JSONObject;
import com.huawei.browsergateway.adapter.FrameworkAdapter;
import com.huawei.browsergateway.adapter.dto.AuditLevel;
import com.huawei.browsergateway.adapter.dto.AuditResult;
import com.huawei.browsergateway.adapter.dto.AuditType;
import com.huawei.browsergateway.adapter.dto.OperateType;
import com.huawei.browsergateway.config.FrameworkStartupConfig;
import com.huawei.browsergateway.util.AuditLogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class BrowserGatewayApplication {
    private static Logger log = LoggerFactory.getLogger(BrowserGatewayApplication.class);

    public static void main(String[] args){
        log.info("BrowserGateway application starting...");
        
        // TODO internal模式下注掉：启动Spring Boot应用
        ApplicationContext context = SpringApplication.run(BrowserGatewayApplication.class, args);
        
        // Framework和OM SDK的初始化通过ApplicationRunner在应用启动后自动执行
        // 获取FrameworkAdapter并初始化
        try {
            FrameworkAdapter frameworkAdapter = context.getBean(FrameworkAdapter.class);
            FrameworkStartupConfig.initializeFramework(frameworkAdapter);
        } catch (Exception e) {
            log.warn("FrameworkAdapter not available, skipping framework initialization", e);
        }

        // 测试审计日志记录
        JSONObject operation = new JSONObject();
        operation.put("OP_EN", "test op");
        operation.put("OP_ZH", "测试操作");

        // 操作日志
        AuditLogUtil.writeAuditLog(AuditType.OPERATION,
                new AuditLogUtil.AuditLogInfo()
                        .setAppName("browsergw")
                        .setDetail("test detail")
                        .setDetailZh("测试明细")
                        .setOperation(operation.toString())
                        .setUserName("test user")
                        .setTerminal("test terminal"),

                AuditLevel.MINOR,
                OperateType.GET,
                AuditResult.SUCCESSFUL
        );

        AuditLogUtil.writeAuditLog(AuditType.SECURITY,
                new AuditLogUtil.AuditLogInfo()
                        .setAppName("browsergw")
                        .setDetail("test detail")
                        .setDetailZh("测试明细")
                        .setOperation(operation.toString())
                        .setUserName("test user")
                        .setTerminal("test terminal"),

                AuditLevel.MINOR,
                OperateType.GET,
                AuditResult.SUCCESSFUL
        );

        log.info("BrowserGateway application started successfully");
    }
}