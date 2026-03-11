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
 * 服务状态刷新任务
 * 定期上报浏览器实例使用情况
 * 
 * 功能说明：
 * 1. 定期调用chromeSet.reportUsed()上报当前使用的浏览器实例数量
 * 2. 用于服务监控和资源统计
 * 
 * @author BrowserGateway
 */
@Component
public class ServiceStatusRefresherTask {
    
    private static final Logger log = LogManager.getLogger(ServiceStatusRefresherTask.class);

    @Autowired
    private IChromeSet chromeSet;

    @Value("${browsergw.scheduled.report-period:30000}")
    private long period;

    private ScheduledExecutorService scheduler;

    /**
     * 初始化定时任务
     */
    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::refreshServiceStatus, 0, period, TimeUnit.MILLISECONDS);
        log.info("Service status refresher task initialized, period: {}ms", period);
    }

    /**
     * 刷新服务状态
     */
    public void refreshServiceStatus() {
        try {
            chromeSet.reportUsed();
        } catch (Exception e) {
            log.error("refresh service status error!", e);
        }
    }

    /**
     * 销毁定时任务
     */
    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            log.info("Service status refresher task destroyed.");
        }
    }
}
