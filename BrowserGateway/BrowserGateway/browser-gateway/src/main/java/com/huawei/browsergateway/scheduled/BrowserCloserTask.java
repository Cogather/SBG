package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.entity.browser.UserChrome;
import com.huawei.browsergateway.service.IChromeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 浏览器关闭任务
 * 关闭长时间无心跳的浏览器实例，释放系统资源
 */
@Component
public class BrowserCloserTask {
    
    private static final Logger log = LoggerFactory.getLogger(BrowserCloserTask.class);
    
    @Autowired
    private IChromeSet chromeSet;
    
    // 浏览器实例TTL（纳秒），默认100小时
    @Value("${browsergw.chrome.ttl:360000000000}")
    private long browserTtl;
    
    // 关闭任务执行周期（默认10分钟）
    @Value("${browsergw.scheduled.close-browser-period:600000}")
    private long closeBrowserPeriod;
    
    /**
     * 关闭超时的浏览器实例
     * 默认每10分钟执行一次
     */
    @Scheduled(fixedDelayString = "${browsergw.scheduled.close-browser-period:600000}")
    public void closeBrowser() {
        log.debug("开始执行浏览器关闭任务");
        
        try {
            long currentTime = System.nanoTime();
            Set<String> allUsers = chromeSet.getAllUser();
            
            if (allUsers.isEmpty()) {
                log.debug("没有活跃的浏览器实例");
                return;
            }
            
            int closedCount = 0;
            for (String userId : allUsers) {
                try {
                    UserChrome userChrome = chromeSet.get(userId);
                    if (userChrome == null) {
                        continue;
                    }
                    
                    long lastHeartbeat = userChrome.getLastHeartbeat();
                    long elapsed = currentTime - lastHeartbeat;
                    
                    // 如果超过TTL，关闭实例
                    if (elapsed > browserTtl) {
                        log.info("浏览器实例超时，准备关闭: userId={}, elapsed={}ns, ttl={}ns", 
                            userId, elapsed, browserTtl);
                        
                        chromeSet.delete(userId);
                        closedCount++;
                    }
                } catch (Exception e) {
                    log.error("关闭浏览器实例异常: userId={}", userId, e);
                }
            }
            
            if (closedCount > 0) {
                log.info("浏览器关闭任务完成，关闭实例数: {}/{}", closedCount, allUsers.size());
            } else {
                log.debug("浏览器关闭任务完成，无需关闭实例");
            }
            
        } catch (Exception e) {
            log.error("浏览器关闭任务执行异常", e);
        }
    }
}
