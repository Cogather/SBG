package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.service.impl.UserBind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 浏览器关闭任务
 * 关闭长时间无心跳的浏览器实例，释放系统资源
 */
@Component
public class BrowserCloserTask {
    private static final Logger log = LogManager.getLogger(BrowserCloserTask.class);

    @Autowired
    private IRemote remote;

    @Autowired
    private IChromeSet chromeSet;

    @Autowired
    private Config config;

    @Value("${browsergw.scheduled.close-browser-period:600000}")
    private long period;

    @Value("${browsergw.chrome.ttl}")
    private long ttl;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::closeBrowser, 0, period, TimeUnit.MILLISECONDS);
    }

    // TODO 可以优化掉， 不需要获取userbind， 因为tcp连接自身可以检测
    public void closeBrowser() {
        log.info("begin scheduled task for monitoring browser instances.");
        List<String> userIds = new ArrayList<>(chromeSet.getAllUser());
        try {
            for (String userId : userIds) {
                try {
                    UserBind ub = remote.getUserBind(userId);
                    if (!isActive(ub) || ifExpired(userId)) {
                        log.info("browser {} is expired, close it.", userId);
                        chromeSet.delete(userId);
                    }
                } catch (Exception e) {
                    log.error("failed to close user {} browser", userId, e);
                }
            }
        } catch (Exception e) {
            log.error("monitoring browser instances error!", e);
        }
    }

    private boolean ifExpired(String userId) {
        return System.nanoTime() - chromeSet.getHeartbeats(userId) > ttl;
    }

    private boolean isActive(UserBind userBind) {
        if (userBind == null) {
            return false;
        }
        if (userBind.getBrowserInstance() == null) {
            return false;
        }
        if (!config.getSelfAddr().equals(userBind.getBrowserInstance())) {
            return false;
        }
        if (userBind.getHeartbeats() == null) {
            return false;
        }
        return true;
    }
    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
