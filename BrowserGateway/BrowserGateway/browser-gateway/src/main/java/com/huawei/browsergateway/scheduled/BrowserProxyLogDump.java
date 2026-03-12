package com.huawei.browsergateway.scheduled;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 浏览器代理日志轮转任务，定期执行 Shell 脚本完成日志转储
 */
@Component
public class BrowserProxyLogDump {

    private static final Logger log = LogManager.getLogger(BrowserProxyLogDump.class);

    /** Shell 脚本路径 */
    @Value("${shell.script.path:/opt/csp/browsergw/module/log_rotate.sh}")
    private String scriptPath;

    /** 执行周期（毫秒），默认 60 分钟 */
    @Value("${shell.script.period:3600000}")
    private long period;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::executeShellScript, 0, period, TimeUnit.MILLISECONDS);
        log.info("Shell script executor task started. Script: {}, Period: {}ms", scriptPath, period);
    }

    /** 执行日志轮转脚本，将标准输出和错误输出合并后逐行记录 */
    private void executeShellScript() {
        log.info("Begin executing shell script: {}", scriptPath);
        try {
            ProcessBuilder builder = new ProcessBuilder("bash", scriptPath);
            builder.redirectErrorStream(true);

            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Script output: {}", line);
                }
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
