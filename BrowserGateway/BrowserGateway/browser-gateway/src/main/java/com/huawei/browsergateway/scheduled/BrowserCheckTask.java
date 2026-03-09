package com.huawei.browsergateway.scheduled;

import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.impl.UserChrome;
import com.huawei.browsergateway.sdk.ClientImpl;
import com.huawei.browsergateway.sdk.DriverClient;
import com.huawei.browsergateway.sdk.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 浏览器检查任务
 * 定期检查浏览器实例状态，清理异常实例
 * 
 * 功能说明：
 * 1. 执行Chrome健康检查
 * 2. 获取所有用户实例
 * 3. 找出异常的浏览器实例（通过proxyContextId匹配错误上下文）
 * 4. 删除异常实例
 * 
 * @author BrowserGateway
 */
@Component
public class BrowserCheckTask {
    
    private static final Logger log = LogManager.getLogger(BrowserCheckTask.class);
    
    @Autowired
    private IChromeSet chromeSet;
    
    @Autowired
    private Config config;
    
    @Value("${browsergw.scheduled.check-browser-period:1800000}")
    private long period;
    
    private ScheduledExecutorService scheduler;
    private DriverClient client;

    /**
     * 初始化定时任务
     */
    @PostConstruct
    public void init() {
        client = new ClientImpl(config.getChrome().getEndpoint());
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkBrowsers, 0, period, TimeUnit.MILLISECONDS);
        log.info("Browser check task initialized, period: {}ms", period);
    }

    /**
     * 检查浏览器状态
     * 执行健康检查，找出异常实例并清理
     */
    public void checkBrowsers() {
        try {
            log.info("begin scheduled task for check browsers status.");
            long start = System.currentTimeMillis();
            
            // 执行Chrome健康检查
            Type.HealthCheckResult result = client.browser().healthCheck();
            if (result.isSuccess()) {
                log.info("check browsers success. cost:{}ms", System.currentTimeMillis() - start);
                return;
            }

            // 获取所有用户实例
            Set<String> users = chromeSet.getAllUser();
            Set<String> delUsers = new HashSet<>();
            
            // 找出异常的浏览器实例
            for (String user : users) {
                UserChrome userChrome = chromeSet.get(user);
                if (userChrome == null) {
                    continue;
                }
                
                String proxyContextId = userChrome.getChromeDriver().getProxyContextId();
                if (result.getErrContexts().contains(proxyContextId)) {
                    delUsers.add(user);
                }
            }
            
            // 删除异常实例
            if (!delUsers.isEmpty()) {
                log.info("These user instances have expired:{}, close.", JSONUtil.toJsonStr(delUsers));
                delUsers.forEach(chromeSet::delete);
            }
            
            log.info("end scheduled task for check browsers status. cost:{}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("check browser task error.", e);
        }
    }

    /**
     * 销毁定时任务
     */
    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            log.info("Browser check task destroyed.");
        }
    }
}
