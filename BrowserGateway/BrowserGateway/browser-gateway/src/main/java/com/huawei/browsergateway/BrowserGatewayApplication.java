package com.huawei.browsergateway;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.log4j2.Log4J2LoggingSystem;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class BrowserGatewayApplication {
    private static final Logger log = LogManager.getLogger(BrowserGatewayApplication.class);

    public static void main(String[] args) {
        // 类路径上若仍残留 Logback（例如 IDE 未刷新依赖），强制使用 Log4j2，避免与 log4j-slf4j-impl 冲突
        System.setProperty(LoggingSystem.SYSTEM_PROPERTY, Log4J2LoggingSystem.class.getName());

        log.info("BrowserGateway application starting...");

//        // 保留原来的适配器启动逻辑，供内网构建场景参考
//        String environment = "external";
//        AdapterFactory adapterFactory = AdapterConfig.getAdapterFactory(environment);
//        FrameworkAdapter frameworkAdapter = adapterFactory.createFrameworkAdapter();
//        boolean frameworkStarted = frameworkAdapter.start();
//        if (!frameworkStarted) {
//            log.error("Framework start failed");
//        }
//
//        JSONObject operation = new JSONObject();
//        operation.put("OP_EN", "test op");
//        operation.put("OP_ZH", "测试操作");
//
//        AuditLogUtil.writeAuditLog(AuditType.OPERATION,
//                new AuditLogUtil.AuditLogInfo()
//                        .setAppName("browsergw")
//                        .setDetail("test detail")
//                        .setDetailZh("测试明细")
//                        .setOperation(operation.toString())
//                        .setUserName("test user")
//                        .setTerminal("test terminal"),
//                AuditLevel.MINOR,
//                OperateType.GET,
//                AuditResult.SUCCESSFUL
//        );
//
//        AuditLogUtil.writeAuditLog(AuditType.SECURITY,
//                new AuditLogUtil.AuditLogInfo()
//                        .setAppName("browsergw")
//                        .setDetail("test detail")
//                        .setDetailZh("测试明细")
//                        .setOperation(operation.toString())
//                        .setUserName("test user")
//                        .setTerminal("test terminal"),
//                AuditLevel.MINOR,
//                OperateType.GET,
//                AuditResult.SUCCESSFUL
//        );

        // todo 内网编译时会注释掉
        ApplicationContext context = SpringApplication.run(BrowserGatewayApplication.class, args);
        log.info("BrowserGateway application started successfully, contextId={}", context.getId());
    }
}