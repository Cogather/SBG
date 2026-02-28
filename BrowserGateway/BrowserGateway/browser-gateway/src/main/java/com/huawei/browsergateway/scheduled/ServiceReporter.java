package com.huawei.browsergateway.scheduled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.huawei.browsergateway.service.IChromeSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ServiceReporter {
    private static final Logger log = LogManager.getLogger(ServiceReporter.class);

    @Autowired
    private IChromeSet chromeSet;

    @EventListener(ContextRefreshedEvent.class)
    public void startReport(ContextRefreshedEvent event) {
        reportChainInfoWithRetry(5);
        log.info("finish report chain endpoints");
    }

    private void reportChainInfoWithRetry(int remainCount) {
        long waitTime = 30000; // 30s间隔
        log.info("start to report chain endpoints");
        if (chromeSet.reportChainEndpoints()) {
            log.info("report chain endpoint success");
            return;
        }
        if (remainCount == 0) { // 达到最大重试次数，依靠自身重试无法恢复，重启进程
            log.fatal("report chain endpoint failed");
        }
        log.info("failed to report,will retry");
        try {
            Thread.sleep(waitTime); // 等待一段时间再重试
        } catch (Exception sleepException) {
            log.fatal("failed to sleep");
        }
        reportChainInfoWithRetry(remainCount - 1);
    }

}
