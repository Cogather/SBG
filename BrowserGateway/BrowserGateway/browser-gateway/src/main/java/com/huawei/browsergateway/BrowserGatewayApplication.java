package com.huawei.browsergateway;

import com.alibaba.fastjson2.JSONObject;
import com.huawei.browsergateway.util.AuditLogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;

public class BrowserGatewayApplication {
    private static Logger log = LoggerFactory.getLogger(BrowserGatewayApplication.class);

    public static void main(String[] args){
        // Framework和OM SDK的初始化已迁移到FrameworkStartupConfig
        // 通过Spring Boot的ApplicationRunner在应用启动后自动执行
        log.info("BrowserGateway application starting...");

        JSONObject operation = new JSONObject();
        operation.put("OP_EN", "test op");
        operation.put("OP_ZH", "测试操作");

        // 操作日志
        AuditLogUtil.writeAuditLog(AuditLogUtil.AuditType.OPERATION,
                new AuditLogUtil.AuditLogInfo()
                        .setAppName("browsergw")
                        .setDetail("test detail")
                        .setDetailZh("测试明细")
                        .setOperation(operation.toString())
                        .setUserName("test user")
                        .setTerminal("test terminal"),

                AuditLogUtil.AuditLevel.MINOR,
                AuditLogUtil.OperateType.GET,
                AuditLogUtil.AuditResult.SUCCESSFUL
        );

        AuditLogUtil.writeAuditLog(AuditLogUtil.AuditType.SECURITY,
                new AuditLogUtil.AuditLogInfo()
                        .setAppName("browsergw")
                        .setDetail("test detail")
                        .setDetailZh("测试明细")
                        .setOperation(operation.toString())
                        .setUserName("test user")
                        .setTerminal("test terminal"),

                AuditLogUtil.AuditLevel.MINOR,
                AuditLogUtil.OperateType.GET,
                AuditLogUtil.AuditResult.SUCCESSFUL
        );
        // 启动 Spring Boot 应用
        SpringApplication.run(BrowserGatewayApplication.class, args);

        log.info("BrowserGateway application started successfully");
    }
}