package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.service.IChromeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 会话清理任务
 * 清理过期的会话数据
 */
public class ServiceStatusRefresherTask {
    private static final Logger log = LogManager.getLogger(ServiceStatusRefresherTask.class);

    @Autowired
    private IChromeSet chromeSet;

    @Value("${browsergw.scheduled.report-period:30000}")
    private long period;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::refreshServiceStatus, 0, period, TimeUnit.MILLISECONDS);
    }

    public void refreshServiceStatus() {
        try {
            chromeSet.reportUsed();
        } catch (Exception e) {
            log.error("refresh service status error!", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}