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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 浏览器实例健康检查任务，定期通过 ChromeDriver 健康检查接口识别并清理异常实例
 */
@Component
public class BrowserCheckTask {

    private static final Logger log = LogManager.getLogger(BrowserCheckTask.class);

    @Autowired
    private IChromeSet chromeSet;
    @Autowired
    private Config config;

    /** 检查周期（毫秒），默认 30 分钟 */
    @Value("${browsergw.scheduled.check-browser-period:1800000}")
    private long period;

    private ScheduledExecutorService scheduler;
    private DriverClient client;

    @PostConstruct
    public void init() {
        client = new ClientImpl(config.getChrome().getEndpoint());
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkBrowsers, 0, period, TimeUnit.MILLISECONDS);
        log.info("Browser check task initialized, period: {}ms", period);
    }

    /**
     * 执行健康检查：若全部正常则直接返回；否则找出错误上下文对应的用户实例并删除
     */
    public void checkBrowsers() {
        try {
            log.info("begin scheduled task for check browsers status.");
            long start = System.currentTimeMillis();

            Type.HealthCheckResult result = client.browser().healthCheck();
            if (result.isSuccess()) {
                log.info("check browsers success. cost:{}ms", System.currentTimeMillis() - start);
                return;
            }

            Set<String> delUsers = new HashSet<>();
            for (String user : chromeSet.getAllUser()) {
                UserChrome userChrome = chromeSet.get(user);
                if (userChrome != null && result.getErrContexts().contains(userChrome.getChromeDriver().getProxyContextId())) {
                    delUsers.add(user);
                }
            }

            if (!delUsers.isEmpty()) {
                log.info("These user instances have expired:{}, close.", JSONUtil.toJsonStr(delUsers));
                delUsers.forEach(chromeSet::delete);
            }

            log.info("end scheduled task for check browsers status. cost:{}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("check browser task error.", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            log.info("Browser check task destroyed.");
        }
    }
}
