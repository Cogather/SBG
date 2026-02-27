package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.entity.browser.UserChrome;
import com.huawei.browsergateway.driver.ChromeDriverProxy;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.sdk.cdp.ClientImpl;
import com.huawei.browsergateway.sdk.cdp.DriverClient;
import com.huawei.browsergateway.sdk.cdp.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 浏览器检查任务
 * 定期检查浏览器实例状态，清理异常实例
 * 
 * 功能说明（按照存量代码实现方式）：
 * 1. 执行Chrome健康检查
 * 2. 获取所有用户实例
 * 3. 找出异常的浏览器实例（通过proxyContextId匹配错误上下文）
 * 4. 删除异常实例
 */
@Component
public class BrowserCheckTask {
    
    private static final Logger log = LoggerFactory.getLogger(BrowserCheckTask.class);
    
    @Autowired
    private IChromeSet chromeSet;
    
    @Value("${browsergw.scheduled.check-browser-period:1800000}")
    private long period;  // 默认30分钟检查一次
    
    @Value("${browsergw.chrome.endpoint:http://127.0.0.1:8000}")
    private String chromeEndpoint;
    
    private ScheduledExecutorService scheduler;
    private DriverClient driverClient;
    
    @PostConstruct
    public void init() {
        // 初始化Chrome健康检查客户端
        driverClient = new ClientImpl(chromeEndpoint);
        log.info("浏览器健康检查任务已初始化，Chrome端点: {}", chromeEndpoint);
    }
    
    /**
     * 执行浏览器检查任务
     * 默认每30分钟执行一次
     */
    @Scheduled(fixedDelayString = "${browsergw.scheduled.check-browser-period:1800000}")
    public void checkBrowsers() {
        try {
            log.info("开始执行浏览器健康检查任务");
            long start = System.currentTimeMillis();
            
            // 1. 执行Chrome健康检查
            Type.HealthCheckResult result = driverClient.browser().healthCheck();
            if (result.isSuccess()) {
                log.info("浏览器健康检查成功，耗时: {}ms", System.currentTimeMillis() - start);
                return;  // 健康检查成功，无需处理
            }
            
            // 2. 获取所有用户实例
            Set<String> users = chromeSet.getAllUser();
            if (users.isEmpty()) {
                log.debug("没有活跃的浏览器实例");
                return;
            }
            
            // 3. 找出异常的浏览器实例
            Set<String> delUsers = new HashSet<>();
            for (String userId : users) {
                try {
                    UserChrome userChrome = chromeSet.get(userId);
                    if (userChrome == null) {
                        continue;
                    }
                    
                    // 获取ChromeDriver并检查proxyContextId
                    Object chromeDriverObj = userChrome.getChromeDriver();
                    if (chromeDriverObj == null) {
                        log.warn("浏览器实例ChromeDriver为空: userId={}", userId);
                        // ChromeDriver为空，视为异常实例
                        delUsers.add(userId);
                        continue;
                    }
                    
                    String proxyContextId = null;
                    if (chromeDriverObj instanceof ChromeDriverProxy) {
                        ChromeDriverProxy chromeDriver = (ChromeDriverProxy) chromeDriverObj;
                        proxyContextId = chromeDriver.getProxyContextId();
                    } else {
                        // 如果不是ChromeDriverProxy类型，尝试使用反射获取
                        try {
                            java.lang.reflect.Method getContextIdMethod = chromeDriverObj.getClass().getMethod("getProxyContextId");
                            Object contextId = getContextIdMethod.invoke(chromeDriverObj);
                            if (contextId != null) {
                                proxyContextId = contextId.toString();
                            }
                        } catch (NoSuchMethodException e) {
                            log.debug("ChromeDriver没有getProxyContextId方法: userId={}", userId);
                            // 如果没有该方法，使用userId作为proxyContextId
                            proxyContextId = userId;
                        }
                    }
                    
                    // 检查上下文ID是否在错误列表中
                    if (proxyContextId != null && result.getErrContexts() != null 
                        && result.getErrContexts().contains(proxyContextId)) {
                        log.warn("发现异常浏览器实例: userId={}, proxyContextId={}", userId, proxyContextId);
                        delUsers.add(userId);
                    }
                } catch (Exception e) {
                    log.error("检查浏览器实例异常: userId={}", userId, e);
                    // 检查失败，视为异常实例
                    delUsers.add(userId);
                }
            }
            
            // 4. 删除异常实例
            if (!delUsers.isEmpty()) {
                log.info("发现异常用户实例，准备清理: {}", delUsers);
                for (String userId : delUsers) {
                    try {
                        chromeSet.delete(userId);
                        log.info("成功清理异常用户实例: userId={}", userId);
                    } catch (Exception e) {
                        log.error("清理异常用户实例失败: userId={}", userId, e);
                    }
                }
            }
            
            log.info("浏览器健康检查任务完成，耗时: {}ms, 清理实例数: {}", 
                System.currentTimeMillis() - start, delUsers.size());
            
        } catch (Exception e) {
            log.error("浏览器健康检查任务执行异常", e);
        }
    }
    
    @PreDestroy
    public void destroy() {
        // 清理资源
        if (driverClient instanceof ClientImpl) {
            ((ClientImpl) driverClient).close();
        }
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("浏览器健康检查任务已停止");
        }
    }
}
