package com.huawei.browsergateway.tcpserver.media;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 媒体流客户端管理
 * 管理所有TCP媒体流连接
 */
public class MediaClientSet {
    private static final Logger log = LoggerFactory.getLogger(MediaClientSet.class);
    
    // 用户ID -> Channel映射
    private final ConcurrentMap<String, Channel> clientChannels = new ConcurrentHashMap<>();
    
    // Channel -> 用户ID映射
    private final ConcurrentMap<Channel, String> channelUsers = new ConcurrentHashMap<>();
    
    // 用户ID -> 最后心跳时间（纳秒）
    private final ConcurrentMap<String, Long> lastHeartbeatMap = new ConcurrentHashMap<>();
    
    /**
     * 添加客户端连接
     */
    public void addClient(String userId, Channel channel) {
        if (userId == null || channel == null) {
            log.warn("添加媒体流客户端失败: userId或channel为null");
            return;
        }
        
        // 如果已存在该用户的连接，先关闭旧连接
        Channel oldChannel = clientChannels.get(userId);
        if (oldChannel != null && oldChannel.isActive()) {
            log.info("关闭旧媒体流连接: userId={}", userId);
            oldChannel.close();
        }
        
        clientChannels.put(userId, channel);
        channelUsers.put(channel, userId);
        updateHeartbeat(userId);
        
        log.info("添加媒体流客户端: userId={}, channel={}", userId, channel.id());
    }
    
    /**
     * 移除客户端连接
     */
    public void removeClient(Channel channel) {
        if (channel == null) {
            return;
        }
        
        String userId = channelUsers.remove(channel);
        if (userId != null) {
            clientChannels.remove(userId);
            lastHeartbeatMap.remove(userId);
            log.info("移除媒体流客户端: userId={}, channel={}", userId, channel.id());
        }
    }
    
    /**
     * 移除客户端连接（通过用户ID）
     */
    public void removeClient(String userId) {
        if (userId == null) {
            return;
        }
        
        Channel channel = clientChannels.remove(userId);
        if (channel != null) {
            channelUsers.remove(channel);
            lastHeartbeatMap.remove(userId);
            log.info("移除媒体流客户端: userId={}", userId);
        }
    }
    
    /**
     * 获取客户端连接
     */
    public Channel getClient(String userId) {
        return clientChannels.get(userId);
    }
    
    /**
     * 获取用户ID
     */
    public String getUserId(Channel channel) {
        return channelUsers.get(channel);
    }
    
    /**
     * 更新心跳时间
     */
    public void updateHeartbeat(String userId) {
        if (userId != null) {
            lastHeartbeatMap.put(userId, System.nanoTime());
        }
    }
    
    /**
     * 获取最后心跳时间
     */
    public Long getLastHeartbeat(String userId) {
        return lastHeartbeatMap.get(userId);
    }
    
    /**
     * 清理超时的客户端连接
     * 
     * @param heartbeatTtl 心跳超时时间（纳秒）
     * @return 清理的连接数
     */
    public int cleanExpiredClients(long heartbeatTtl) {
        long now = System.nanoTime();
        int cleanedCount = 0;
        
        for (ConcurrentMap.Entry<String, Long> entry : lastHeartbeatMap.entrySet()) {
            String userId = entry.getKey();
            Long lastHeartbeat = entry.getValue();
            
            if (lastHeartbeat != null && (now - lastHeartbeat > heartbeatTtl)) {
                log.info("清理超时媒体流客户端: userId={}, 超时时间={}ns", userId, now - lastHeartbeat);
                Channel channel = clientChannels.get(userId);
                if (channel != null && channel.isActive()) {
                    channel.close();
                }
                removeClient(userId);
                cleanedCount++;
            }
        }
        
        if (cleanedCount > 0) {
            log.info("清理超时媒体流客户端完成: 清理数量={}", cleanedCount);
        }
        
        return cleanedCount;
    }
    
    /**
     * 获取当前连接数
     */
    public int getClientCount() {
        return clientChannels.size();
    }
    
    /**
     * 检查客户端是否存在
     */
    public boolean containsClient(String userId) {
        return clientChannels.containsKey(userId);
    }
}
