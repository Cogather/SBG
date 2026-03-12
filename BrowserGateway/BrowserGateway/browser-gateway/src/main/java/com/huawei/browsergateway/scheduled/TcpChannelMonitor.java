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
 * TCP 连接心跳监控任务，定期扫描控制通道和媒体通道，清理心跳超时的连接
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

    /** 检查周期（毫秒），默认 10 分钟 */
    @Value("${browsergw.scheduled.tcp-heartbeat-period:600000}")
    private long period;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tcpClientMonitor, 0, period, TimeUnit.MILLISECONDS);
        log.info("TCP channel monitor task initialized, period: {}ms", period);
    }

    /** 检查控制通道和媒体通道的心跳，移除超时连接 */
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
     * 遍历客户端集合，将心跳超时的连接收集后统一删除
     *
     * @param clientSet 待检查的客户端集合
     * @param ttl       心跳超时阈值（纳秒）
     */
    private static void checkTcpHeartbeat(ClientSet clientSet, long ttl) {
        Set<String> expired = new HashSet<>();
        clientSet.allClient().forEach(key -> {
            Client client = clientSet.get(key);
            if (client != null && System.nanoTime() - client.getTime(Client.VAL_HEARTBEAT_TIME) > ttl) {
                log.info("client {} is expired, close it.", key);
                expired.add(key);
            }
        });
        expired.forEach(clientSet::del);
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
            log.info("TCP channel monitor task destroyed.");
        }
    }
}
