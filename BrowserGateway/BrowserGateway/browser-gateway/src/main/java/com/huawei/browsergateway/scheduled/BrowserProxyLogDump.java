package com.huawei.browsergateway.scheduled;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 浏览器代理日志转储任务
 * 定期执行Shell脚本进行日志轮转
 * 
 * 功能说明：
 * 1. 定期执行配置的Shell脚本
 * 2. 记录脚本执行输出
 * 3. 处理脚本执行异常
 * 
 * @author BrowserGateway
 */
@Component
public class BrowserProxyLogDump {

    private static final Logger log = LogManager.getLogger(BrowserProxyLogDump.class);

    /**
     * Shell脚本路径，从配置文件中读取
     */
    @Value("${shell.script.path:/opt/csp/browsergw/module/log_rotate.sh}")
    private String scriptPath;

    /**
     * 执行周期，默认60分钟
     */
    @Value("${shell.script.period:3600000}")
    private long period;

    private ScheduledExecutorService scheduler;

    /**
     * 初始化定时任务
     */
    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::executeShellScript, 0, period, TimeUnit.MILLISECONDS);
        log.info("Shell script executor task started. Script: {}, Period: {}ms", scriptPath, period);
    }

    /**
     * 执行Shell脚本
     */
    private void executeShellScript() {
        log.info("Begin executing shell script: {}", scriptPath);
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("bash", scriptPath);  // 执行bash脚本
            builder.redirectErrorStream(true);     // 合并错误输出到标准输出

            Process process = builder.start();
            
            // 读取脚本输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Script output: {}", line);
                }
            }

            // 等待脚本执行完成
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Shell script executed successfully.");
            } else {
                log.warn("Shell script exited with code: {}", exitCode);
            }

        } catch (Exception e) {
            log.error("Failed to execute shell script: {}", scriptPath, e);
        }
    }

    /**
     * 销毁定时任务
     */
    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            log.info("Shell script executor task stopped.");
        }
    }
}
