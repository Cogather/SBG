package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.service.impl.UserBind;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 浏览器关闭任务
 * 关闭长时间无心跳的浏览器实例，释放系统资源
 * 
 * 功能说明：
 * 1. 定期检查所有浏览器实例
 * 2. 判断实例是否活跃（通过UserBind检查）
 * 3. 判断实例是否过期（通过心跳时间检查）
 * 4. 关闭过期或不活跃的实例
 * 
 * @author BrowserGateway
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

    /**
     * 初始化定时任务
     */
    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::closeBrowser, 0, period, TimeUnit.MILLISECONDS);
        log.info("Browser closer task initialized, period: {}ms, ttl: {}ns", period, ttl);
    }

    /**
     * 关闭过期的浏览器实例
     */
    public void closeBrowser() {
        log.info("begin scheduled task for monitoring browser instances.");
        try {
            List<String> userIds = new ArrayList<>(chromeSet.getAllUser());
            for (String userId : userIds) {
                processUserBrowser(userId);
            }
        } catch (Exception e) {
            log.error("monitoring browser instances error!", e);
        }
    }

    /**
     * 处理单个用户的浏览器实例
     * 检查是否过期或不活跃，如果是则关闭
     * 
     * @param userId 用户ID
     */
    private void processUserBrowser(String userId) {
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

    /**
     * 判断浏览器实例是否过期
     * 
     * @param userId 用户ID
     * @return true表示已过期，false表示未过期
     */
    private boolean ifExpired(String userId) {
        long currentTime = System.nanoTime();
        long heartbeats = chromeSet.getHeartbeats(userId);
        return currentTime - heartbeats > ttl;
    }

    /**
     * 判断浏览器实例是否活跃
     * 
     * @param userBind 用户绑定信息
     * @return true表示活跃，false表示不活跃
     */
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

    /**
     * 销毁定时任务
     */
    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            log.info("Browser closer task destroyed.");
        }
    }
}
