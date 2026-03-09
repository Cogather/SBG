package com.huawei.browsergateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class BrowserGatewayApplication {
    private static final Logger log = LoggerFactory.getLogger(BrowserGatewayApplication.class);

    public static void main(String[] args) {
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