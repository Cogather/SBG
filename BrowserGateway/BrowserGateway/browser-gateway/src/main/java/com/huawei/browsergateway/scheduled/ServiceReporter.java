package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.service.IChromeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 服务上报器
 * 在应用启动时上报链端点信息
 * 
 * 功能说明：
 * 1. 监听应用上下文刷新事件
 * 2. 上报链端点信息
 * 3. 支持重试机制（最多重试5次）
 * 
 * @author BrowserGateway
 */
@Service
public class ServiceReporter {
    
    private static final Logger log = LogManager.getLogger(ServiceReporter.class);

    @Autowired
    private IChromeSet chromeSet;

    /**
     * 监听应用上下文刷新事件，启动上报
     * 
     * @param event 上下文刷新事件
     */
    @EventListener(ContextRefreshedEvent.class)
    public void startReport(ContextRefreshedEvent event) {
        reportChainInfoWithRetry(5);
        log.info("finish report chain endpoints");
    }

    /**
     * 带重试机制的上报链端点信息
     * 
     * @param remainCount 剩余重试次数
     */
    private void reportChainInfoWithRetry(int remainCount) {
        long waitTime = 30000; // 30秒间隔
        log.info("start to report chain endpoints");
        
        if (chromeSet.reportChainEndpoints()) {
            log.info("report chain endpoint success");
            return;
        }
        
        if (remainCount == 0) {
            // 达到最大重试次数，依靠自身重试无法恢复，重启进程
            log.fatal("report chain endpoint failed");
        }
        
        log.info("failed to report, will retry");
        try {
            Thread.sleep(waitTime); // 等待一段时间再重试
        } catch (Exception sleepException) {
            log.fatal("failed to sleep", sleepException);
        }
        
        reportChainInfoWithRetry(remainCount - 1);
    }
}
