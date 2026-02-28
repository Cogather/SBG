package com.huawei.browsergateway.scheduled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class BrowserProxyLogDump {

    private static final Logger log = LogManager.getLogger(BrowserProxyLogDump.class);

    // 从配置文件中读取脚本路径和执行周期
    @Value("${shell.script.path:/opt/csp/browsergw/module/log_rotate.sh}")
    private String scriptPath;

    @Value("${shell.script.period:3600000}")
    private long period; // 60分钟执行一次

    private ScheduledExecutorService scheduler;

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
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("Script output: {}", line);
            }

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

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            log.info("Shell script executor task stopped.");
        }
    }
}
