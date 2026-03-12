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
 * 浏览器实例超时关闭任务，定期检查所有实例的活跃状态和心跳时间，
 * 对不活跃或心跳超时的实例执行关闭操作
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

    /** 检查周期（毫秒），默认 10 分钟 */
    @Value("${browsergw.scheduled.close-browser-period:600000}")
    private long period;

    /** 浏览器实例心跳超时阈值（纳秒） */
    @Value("${browsergw.chrome.ttl}")
    private long ttl;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::closeBrowser, 0, period, TimeUnit.MILLISECONDS);
        log.info("Browser closer task initialized, period: {}ms, ttl: {}ns", period, ttl);
    }

    /** 遍历所有用户实例，关闭不活跃或心跳超时的实例 */
    public void closeBrowser() {
        log.info("begin scheduled task for monitoring browser instances.");
        List<String> userIds = new ArrayList<>(chromeSet.getAllUser());
        try {
            for (String userId : userIds) {
                try {
                    UserBind ub = remote.getUserBind(userId);
                    if (!isActive(ub) || isExpired(userId)) {
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

    /**
     * 判断心跳是否超时
     *
     * @param userId 用户 ID
     * @return true 表示已超时
     */
    private boolean isExpired(String userId) {
        return System.nanoTime() - chromeSet.getHeartbeats(userId) > ttl;
    }

    /**
     * 判断用户绑定是否有效（绑定到本实例且心跳记录存在）
     *
     * @param userBind 用户绑定信息
     * @return true 表示活跃
     */
    private boolean isActive(UserBind userBind) {
        return userBind != null
                && userBind.getBrowserInstance() != null
                && config.getSelfAddr().equals(userBind.getBrowserInstance())
                && userBind.getHeartbeats() != null;
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            log.info("Browser closer task destroyed.");
        }
    }
}
