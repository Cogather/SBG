package com.huawei.browsergateway.tcpserver.control;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 控制流客户端管理
 * 管理所有TCP控制流连接
 */
public class ControlClientSet {
    private static final Logger log = LoggerFactory.getLogger(ControlClientSet.class);
    
    // 用户ID -> Channel映射
    private final ConcurrentMap<String, Channel> clientChannels = new ConcurrentHashMap<>();
    
    // Channel -> 用户ID映射
    private final ConcurrentMap<Channel, String> channelUsers = new ConcurrentHashMap<>();
    
    // 用户ID -> 最后心跳时间（纳秒）
    private final ConcurrentMap<String, Long> lastHeartbeatMap = new ConcurrentHashMap<>();
    
    // 用户ID -> 连接状态
    private final ConcurrentMap<String, ConnectionState> connectionStates = new ConcurrentHashMap<>();
    
    // 用户ID -> 重连任务
    private final ConcurrentMap<String, ScheduledFuture<?>> reconnectTasks = new ConcurrentHashMap<>();
    
    // 用户ID -> 重连次数
    private final ConcurrentMap<String, AtomicInteger> reconnectCounts = new ConcurrentHashMap<>();
    
    // 心跳监控任务
    private ScheduledExecutorService heartbeatExecutor;
    
    // 重连调度器
    private ScheduledExecutorService reconnectExecutor;
    
    // 最大重连次数
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    
    // 初始重连延迟（毫秒）
    private static final long INITIAL_RECONNECT_DELAY = 1000;
    
    // 最大重连延迟（毫秒）
    private static final long MAX_RECONNECT_DELAY = 60000;
    
    /**
     * 初始化连接管理
     */
    public void initialize() {
        if (heartbeatExecutor == null) {
            heartbeatExecutor = Executors.newScheduledThreadPool(1, 
                r -> new Thread(r, "ControlClientSet-Heartbeat"));
        }
        if (reconnectExecutor == null) {
            reconnectExecutor = Executors.newScheduledThreadPool(2,
                r -> new Thread(r, "ControlClientSet-Reconnect"));
        }
        log.info("ControlClientSet初始化完成");
    }
    
    /**
     * 关闭连接管理
     */
    public void shutdown() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
        }
        if (reconnectExecutor != null) {
            reconnectExecutor.shutdown();
        }
        log.info("ControlClientSet已关闭");
    }
    
    /**
     * 添加客户端连接
     */
    public void addClient(String userId, Channel channel) {
        addClient(userId, channel, null);
    }
    
    /**
     * 添加客户端连接（带认证）
     */
    public void addClient(String userId, Channel channel, String token) {
        if (userId == null || channel == null) {
            log.warn("添加客户端失败: userId或channel为null");
            return;
        }
        
        // 验证连接
        if (!validateConnection(userId, channel, token)) {
            log.warn("连接验证失败，关闭连接: userId={}", userId);
            channel.close();
            return;
        }
        
        // 如果已存在该用户的连接，先关闭旧连接
        Channel oldChannel = clientChannels.get(userId);
        if (oldChannel != null && oldChannel.isActive()) {
            log.info("关闭旧连接: userId={}", userId);
            oldChannel.close();
        }
        
        // 取消重连任务（如果存在）
        cancelReconnectTask(userId);
        
        clientChannels.put(userId, channel);
        channelUsers.put(channel, userId);
        connectionStates.put(userId, ConnectionState.AUTHENTICATED);
        updateHeartbeat(userId);
        
        // 重置重连次数
        reconnectCounts.remove(userId);
        
        log.info("添加控制流客户端: userId={}, channel={}, state={}", 
            userId, channel.id(), ConnectionState.AUTHENTICATED);
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
            connectionStates.put(userId, ConnectionState.DISCONNECTED);
            log.info("移除控制流客户端: userId={}, channel={}", userId, channel.id());
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
            connectionStates.put(userId, ConnectionState.DISCONNECTED);
            log.info("移除控制流客户端: userId={}", userId);
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
                log.info("清理超时控制流客户端: userId={}, 超时时间={}ns", userId, now - lastHeartbeat);
                Channel channel = clientChannels.get(userId);
                if (channel != null && channel.isActive()) {
                    channel.close();
                }
                removeClient(userId);
                cleanedCount++;
            }
        }
        
        if (cleanedCount > 0) {
            log.info("清理超时控制流客户端完成: 清理数量={}", cleanedCount);
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
    
    /**
     * 向客户端发送数据
     * 根据Moon-SDK文档：检查TCP客户端连接并发送二进制数据
     * 
     * @param userId 用户ID
     * @param data 二进制数据
     * @return 是否发送成功
     */
    public boolean sendToClient(String userId, byte[] data) {
        if (userId == null || data == null) {
            log.warn("发送数据失败: userId或data为null");
            return false;
        }
        
        Channel channel = clientChannels.get(userId);
        if (channel == null || !channel.isActive()) {
            log.warn("客户端连接不存在或未激活: userId={}", userId);
            return false;
        }
        
        try {
            channel.writeAndFlush(data);
            log.debug("数据发送成功: userId={}, size={}", userId, data.length);
            return true;
        } catch (Exception e) {
            log.error("发送数据异常: userId={}", userId, e);
            return false;
        }
    }
    
    /**
     * 验证连接
     * 
     * @param userId 用户ID
     * @param channel 通道
     * @param token 认证令牌（可选）
     * @return 是否验证通过
     */
    private boolean validateConnection(String userId, Channel channel, String token) {
        if (channel == null || !channel.isActive()) {
            log.warn("连接验证失败: channel无效");
            return false;
        }
        
        // 使用ConnectionAuthenticator验证
        String remoteAddress = channel.remoteAddress() != null ? 
            channel.remoteAddress().toString() : "unknown";
        return ConnectionAuthenticator.validateConnection(userId, token, remoteAddress);
    }
    
    /**
     * 启动心跳监控
     * 
     * @param heartbeatInterval 心跳检查间隔（毫秒）
     * @param heartbeatTimeout 心跳超时时间（纳秒）
     */
    public void startHeartbeatMonitoring(long heartbeatInterval, long heartbeatTimeout) {
        if (heartbeatExecutor == null) {
            initialize();
        }
        
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                monitorHeartbeats(heartbeatTimeout);
            } catch (Exception e) {
                log.error("心跳监控异常", e);
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
        
        log.info("心跳监控已启动: interval={}ms, timeout={}ns", heartbeatInterval, heartbeatTimeout);
    }
    
    /**
     * 停止心跳监控
     */
    public void stopHeartbeatMonitoring() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdown();
            heartbeatExecutor = null;
        }
        log.info("心跳监控已停止");
    }
    
    /**
     * 监控心跳
     * 
     * @param heartbeatTimeout 心跳超时时间（纳秒）
     */
    private void monitorHeartbeats(long heartbeatTimeout) {
        long now = System.nanoTime();
        int expiredCount = 0;
        
        for (ConcurrentMap.Entry<String, Long> entry : lastHeartbeatMap.entrySet()) {
            String userId = entry.getKey();
            Long lastHeartbeat = entry.getValue();
            
            if (lastHeartbeat != null && (now - lastHeartbeat > heartbeatTimeout)) {
                log.warn("心跳超时: userId={}, 超时时间={}ns", userId, now - lastHeartbeat);
                expiredCount++;
                
                // 关闭超时连接
                Channel channel = clientChannels.get(userId);
                if (channel != null && channel.isActive()) {
                    channel.close();
                }
                
                // 更新连接状态
                connectionStates.put(userId, ConnectionState.DISCONNECTED);
                
                // 触发重连（如果需要）
                // scheduleReconnect(userId);
            }
        }
        
        if (expiredCount > 0) {
            log.info("心跳监控完成: 超时连接数={}", expiredCount);
        }
    }
    
    /**
     * 安排重连任务（指数退避策略）
     * 
     * @param userId 用户ID
     */
    public void scheduleReconnect(String userId) {
        if (userId == null) {
            return;
        }
        
        // 取消现有重连任务
        cancelReconnectTask(userId);
        
        // 获取重连次数
        AtomicInteger count = reconnectCounts.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int attemptCount = count.get();
        
        if (attemptCount >= MAX_RECONNECT_ATTEMPTS) {
            log.warn("达到最大重连次数，停止重连: userId={}, attempts={}", userId, attemptCount);
            connectionStates.put(userId, ConnectionState.CLOSED);
            return;
        }
        
        // 计算重连延迟（指数退避）
        long delay = Math.min(INITIAL_RECONNECT_DELAY * (1L << attemptCount), MAX_RECONNECT_DELAY);
        
        // 更新连接状态
        connectionStates.put(userId, ConnectionState.RECONNECTING);
        
        // 安排重连任务
        if (reconnectExecutor == null) {
            initialize();
        }
        
        ScheduledFuture<?> future = reconnectExecutor.schedule(() -> {
            try {
                log.info("执行重连: userId={}, attempt={}", userId, attemptCount + 1);
                // 这里可以添加实际的重连逻辑
                // 例如：重新建立连接、发送认证消息等
                count.incrementAndGet();
            } catch (Exception e) {
                log.error("重连失败: userId={}", userId, e);
                // 重连失败，继续安排下一次重连
                scheduleReconnect(userId);
            }
        }, delay, TimeUnit.MILLISECONDS);
        
        reconnectTasks.put(userId, future);
        log.info("安排重连任务: userId={}, delay={}ms, attempt={}", userId, delay, attemptCount + 1);
    }
    
    /**
     * 取消重连任务
     * 
     * @param userId 用户ID
     */
    private void cancelReconnectTask(String userId) {
        ScheduledFuture<?> future = reconnectTasks.remove(userId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.debug("取消重连任务: userId={}", userId);
        }
    }
    
    /**
     * 获取连接状态
     * 
     * @param userId 用户ID
     * @return 连接状态
     */
    public ConnectionState getConnectionState(String userId) {
        return connectionStates.getOrDefault(userId, ConnectionState.DISCONNECTED);
    }
    
    /**
     * 检查连接是否已认证
     * 
     * @param userId 用户ID
     * @return 是否已认证
     */
    public boolean isAuthenticated(String userId) {
        ConnectionState state = getConnectionState(userId);
        return state == ConnectionState.AUTHENTICATED;
    }
}
