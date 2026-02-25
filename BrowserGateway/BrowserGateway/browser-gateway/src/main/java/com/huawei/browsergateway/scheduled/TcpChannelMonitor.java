package com.huawei.browsergateway.scheduled;

import com.huawei.browsergateway.tcpserver.control.ControlClientSet;
import com.huawei.browsergateway.tcpserver.media.MediaClientSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * TCP连接监控任务
 * 监控TCP连接的心跳状态，清理超时连接，统计流量
 */
@Component
public class TcpChannelMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(TcpChannelMonitor.class);
    
    @Autowired
    private ControlClientSet controlClientSet;
    
    @Autowired
    private MediaClientSet mediaClientSet;
    
    // TCP心跳超时时间（纳秒），默认100小时
    @Value("${browsergw.tcp.heartbeat-ttl:360000000000}")
    private long heartbeatTtl;
    
    // TCP监控任务执行周期（默认10分钟）
    @Value("${browsergw.scheduled.tcp-heartbeat-period:600000}")
    private long tcpHeartbeatPeriod;
    
    /**
     * 监控TCP连接并清理超时连接
     * 默认每10分钟执行一次
     */
    @Scheduled(fixedDelayString = "${browsergw.scheduled.tcp-heartbeat-period:600000}")
    public void tcpClientMonitor() {
        log.debug("开始执行TCP连接监控任务");
        
        try {
            // 清理控制流超时连接
            int controlClosedCount = controlClientSet.cleanExpiredClients(heartbeatTtl);
            
            // 清理媒体流超时连接
            int mediaClosedCount = mediaClientSet.cleanExpiredClients(heartbeatTtl);
            
            // 统计连接数
            int controlClientCount = controlClientSet.getClientCount();
            int mediaClientCount = mediaClientSet.getClientCount();
            
            log.info("TCP连接监控任务完成 - 控制流: 总数={}, 清理={}; 媒体流: 总数={}, 清理={}", 
                controlClientCount, controlClosedCount, mediaClientCount, mediaClosedCount);
            
        } catch (Exception e) {
            log.error("TCP连接监控任务执行异常", e);
        }
    }
}
