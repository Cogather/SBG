package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.ClientSet;
import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
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
 * TCP连接监控任务
 * 监控TCP连接的心跳状态，清理超时连接，统计流量
 * 
 * 功能说明：
 * 1. 定期检查控制通道和媒体通道的TCP连接
 * 2. 根据心跳时间判断连接是否超时
 * 3. 清理超时的连接
 * 
 * @author BrowserGateway
 */
@Component
public class TcpChannelMonitor {
    
    private static final Logger log = LogManager.getLogger(TcpChannelMonitor.class);

    @Autowired
    private ControlClientSet controlClientSet;

    @Autowired
    private MediaClientSet mediaClientSet;

    @Autowired
    private Config config;

    @Value("${browsergw.scheduled.tcp-heartbeat-period:600000}")
    private long period;

    private ScheduledExecutorService scheduler;

    /**
     * 初始化定时任务
     */
    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tcpClientMonitor, 0, period, TimeUnit.MILLISECONDS);
        log.info("TCP channel monitor task initialized, period: {}ms", period);
    }

    /**
     * 监控TCP客户端心跳
     */
    public void tcpClientMonitor() {
        log.info("begin scheduled task for monitoring TCP client heartbeats.");
        try {
            long heartbeatTtl = config.getTcp().getHeartbeatTtl();
            checkTcpHeartbeat(controlClientSet, heartbeatTtl);
            checkTcpHeartbeat(mediaClientSet, heartbeatTtl);
        } catch (Exception e) {
            log.error("monitoring TCP client heartbeats error!", e);
        }
    }

    /**
     * 检查TCP连接心跳
     * 
     * @param clientSet 客户端集合
     * @param ttl 心跳超时时间（纳秒）
     */
    private static void checkTcpHeartbeat(ClientSet clientSet, long ttl) {
        Set<String> deleteKeys = new HashSet<>();
        long currentTime = System.nanoTime();
        
        clientSet.allClient().forEach(key -> {
            Client client = clientSet.get(key);
            if (client == null) {
                return;
            }
            
            long heartbeatTime = client.getTime(Client.VAL_HEARTBEAT_TIME);
            if (currentTime - heartbeatTime > ttl) {
                log.info("client {} is expired, close it.", key);
                deleteKeys.add(key);
            }
        });

        // 删除超时的连接
        deleteKeys.forEach(clientSet::del);
    }

    /**
     * 销毁定时任务
     */
    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            log.info("TCP channel monitor task destroyed.");
        }
    }
}
