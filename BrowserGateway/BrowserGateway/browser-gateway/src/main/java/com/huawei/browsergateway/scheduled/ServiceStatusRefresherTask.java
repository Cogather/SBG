package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.service.IChromeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 服务状态定时刷新任务，周期性上报当前浏览器实例使用数量到 CSE
 */
@Component
public class ServiceStatusRefresherTask {

    private static final Logger log = LogManager.getLogger(ServiceStatusRefresherTask.class);

    @Autowired
    private IChromeSet chromeSet;

    /** 上报周期（毫秒），默认 30 秒 */
    @Value("${browsergw.scheduled.report-period:30000}")
    private long period;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::refreshServiceStatus, 0, period, TimeUnit.MILLISECONDS);
        log.info("Service status refresher task initialized, period: {}ms", period);
    }

    /** 上报当前实例使用情况，异常时记录日志但不中断调度 */
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
            log.info("Service status refresher task destroyed.");
        }
    }
}
