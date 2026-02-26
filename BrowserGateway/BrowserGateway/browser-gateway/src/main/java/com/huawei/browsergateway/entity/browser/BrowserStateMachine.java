package com.huawei.browsergateway.entity.browser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 浏览器状态机
 * 管理浏览器实例的状态转换，确保状态转换的合法性和一致性
 */
public class BrowserStateMachine {
    
    private static final Logger log = LoggerFactory.getLogger(BrowserStateMachine.class);
    
    /**
     * 状态转换矩阵
     * 定义从当前状态可以转换到哪些状态
     */
    private static final Map<UserChrome.BrowserStatus, Set<UserChrome.BrowserStatus>> validTransitions = new ConcurrentHashMap<>();
    
    static {
        // 初始化状态转换规则
        validTransitions.put(UserChrome.BrowserStatus.INITIALIZING, Set.of(
            UserChrome.BrowserStatus.PRE_OPENING,
            UserChrome.BrowserStatus.CREATING,
            UserChrome.BrowserStatus.OPEN_ERROR
        ));
        
        validTransitions.put(UserChrome.BrowserStatus.PRE_OPENING, Set.of(
            UserChrome.BrowserStatus.READY,
            UserChrome.BrowserStatus.CONNECTING,
            UserChrome.BrowserStatus.OPEN_ERROR,
            UserChrome.BrowserStatus.CLOSED
        ));
        
        validTransitions.put(UserChrome.BrowserStatus.CREATING, Set.of(
            UserChrome.BrowserStatus.READY,
            UserChrome.BrowserStatus.OPEN_ERROR
        ));
        
        validTransitions.put(UserChrome.BrowserStatus.READY, Set.of(
            UserChrome.BrowserStatus.CONNECTING,
            UserChrome.BrowserStatus.CLOSING,
            UserChrome.BrowserStatus.CLOSED
        ));
        
        validTransitions.put(UserChrome.BrowserStatus.CONNECTING, Set.of(
            UserChrome.BrowserStatus.CONNECTED,
            UserChrome.BrowserStatus.RUNNING,
            UserChrome.BrowserStatus.CONNECTION_ERROR,
            UserChrome.BrowserStatus.CLOSING,
            UserChrome.BrowserStatus.CLOSED
        ));
        
        validTransitions.put(UserChrome.BrowserStatus.CONNECTED, Set.of(
            UserChrome.BrowserStatus.RUNNING,
            UserChrome.BrowserStatus.RECORDING,
            UserChrome.BrowserStatus.CLOSING,
            UserChrome.BrowserStatus.CLOSED
        ));
        
        validTransitions.put(UserChrome.BrowserStatus.RUNNING, Set.of(
            UserChrome.BrowserStatus.READY,
            UserChrome.BrowserStatus.RECORDING,
            UserChrome.BrowserStatus.CLOSING,
            UserChrome.BrowserStatus.CLOSED,
            UserChrome.BrowserStatus.PAGE_CONTROL_ERROR,
            UserChrome.BrowserStatus.NETWORK_ERROR,
            UserChrome.BrowserStatus.MEMORY_ERROR
        ));
        
        validTransitions.put(UserChrome.BrowserStatus.RECORDING, Set.of(
            UserChrome.BrowserStatus.RUNNING,
            UserChrome.BrowserStatus.CLOSING,
            UserChrome.BrowserStatus.CLOSED,
            UserChrome.BrowserStatus.NETWORK_ERROR,
            UserChrome.BrowserStatus.MEMORY_ERROR
        ));
        
        validTransitions.put(UserChrome.BrowserStatus.CLOSING, Set.of(
            UserChrome.BrowserStatus.CLOSED
        ));
        
        // 错误状态可以转换到CLOSED或CLOSING
        validTransitions.put(UserChrome.BrowserStatus.OPEN_ERROR, Set.of(
            UserChrome.BrowserStatus.CLOSING,
            UserChrome.BrowserStatus.CLOSED
        ));
        
        validTransitions.put(UserChrome.BrowserStatus.CONNECTION_ERROR, Set.of(
            UserChrome.BrowserStatus.CLOSING,
            UserChrome.BrowserStatus.CLOSED,
            UserChrome.BrowserStatus.READY  // 可以重试
        ));
        
        validTransitions.put(UserChrome.BrowserStatus.PAGE_CONTROL_ERROR, Set.of(
            UserChrome.BrowserStatus.CLOSING,
            UserChrome.BrowserStatus.CLOSED,
            UserChrome.BrowserStatus.READY  // 可以重试
        ));
        
        validTransitions.put(UserChrome.BrowserStatus.NETWORK_ERROR, Set.of(
            UserChrome.BrowserStatus.CLOSING,
            UserChrome.BrowserStatus.CLOSED,
            UserChrome.BrowserStatus.READY  // 可以重试
        ));
        
        validTransitions.put(UserChrome.BrowserStatus.MEMORY_ERROR, Set.of(
            UserChrome.BrowserStatus.CLOSING,
            UserChrome.BrowserStatus.CLOSED
        ));
        
        // CLOSED状态可以转换到READY（支持重启）
        validTransitions.put(UserChrome.BrowserStatus.CLOSED, Set.of(
            UserChrome.BrowserStatus.READY,
            UserChrome.BrowserStatus.INITIALIZING
        ));
    }
    
    /**
     * 状态转换历史记录
     * 用于调试和问题排查
     */
    private static final Map<String, List<StateTransition>> stateHistory = new ConcurrentHashMap<>();
    
    /**
     * 状态转换记录
     */
    private static class StateTransition {
        final UserChrome.BrowserStatus from;
        final UserChrome.BrowserStatus to;
        final long timestamp;
        final String reason;
        
        StateTransition(UserChrome.BrowserStatus from, UserChrome.BrowserStatus to, String reason) {
            this.from = from;
            this.to = to;
            this.timestamp = System.currentTimeMillis();
            this.reason = reason;
        }
        
        @Override
        public String toString() {
            return String.format("%s -> %s at %d (reason: %s)", from, to, timestamp, reason);
        }
    }
    
    /**
     * 验证状态转换是否合法
     * 
     * @param current 当前状态
     * @param next 目标状态
     * @return 是否可以转换
     */
    public static boolean canTransition(UserChrome.BrowserStatus current, UserChrome.BrowserStatus next) {
        if (current == null || next == null) {
            log.warn("状态转换验证失败: 当前状态或目标状态为null");
            return false;
        }
        
        // 相同状态允许转换（幂等）
        if (current == next) {
            return true;
        }
        
        Set<UserChrome.BrowserStatus> validNextStates = validTransitions.get(current);
        if (validNextStates == null) {
            log.warn("状态转换验证失败: 当前状态 {} 没有定义转换规则", current);
            return false;
        }
        
        boolean canTransition = validNextStates.contains(next);
        if (!canTransition) {
            log.warn("状态转换验证失败: {} -> {} 不是合法转换", current, next);
        }
        
        return canTransition;
    }
    
    /**
     * 执行状态转换
     * 验证转换合法性，记录转换历史，更新状态，触发状态变化事件
     * 
     * @param userChrome 浏览器实例
     * @param nextStatus 目标状态
     * @throws IllegalStateException 如果状态转换不合法
     */
    public static void transition(UserChrome userChrome, UserChrome.BrowserStatus nextStatus) {
        transition(userChrome, nextStatus, null);
    }
    
    /**
     * 执行状态转换（带原因）
     * 
     * @param userChrome 浏览器实例
     * @param nextStatus 目标状态
     * @param reason 转换原因
     * @throws IllegalStateException 如果状态转换不合法
     */
    public static void transition(UserChrome userChrome, UserChrome.BrowserStatus nextStatus, String reason) {
        if (userChrome == null) {
            throw new IllegalArgumentException("浏览器实例不能为null");
        }
        
        UserChrome.BrowserStatus currentStatus = userChrome.getStatus();
        if (currentStatus == null) {
            log.warn("浏览器实例状态为null，设置为初始状态: userId={}", userChrome.getUserId());
            currentStatus = UserChrome.BrowserStatus.INITIALIZING;
            userChrome.setStatus(currentStatus);
        }
        
        // 验证状态转换
        if (!canTransition(currentStatus, nextStatus)) {
            throw new IllegalStateException(
                String.format("非法的状态转换: userId=%s, %s -> %s", 
                    userChrome.getUserId(), currentStatus, nextStatus)
            );
        }
        
        // 如果状态相同，直接返回（幂等）
        if (currentStatus == nextStatus) {
            log.debug("状态未变化，跳过转换: userId={}, status={}", userChrome.getUserId(), currentStatus);
            return;
        }
        
        // 记录状态转换历史
        logStateTransition(userChrome.getUserId(), currentStatus, nextStatus, reason);
        
        // 更新状态
        userChrome.setStatus(nextStatus);
        
        // 触发状态变化事件
        fireStateChangeEvent(userChrome, currentStatus, nextStatus);
    }
    
    /**
     * 尝试状态转换或重启
     * 如果当前状态不允许转换到目标状态，但可以从CLOSED状态转换，则先转换到CLOSED再转换到目标状态
     * 
     * @param userChrome 浏览器实例
     * @param targetStatus 目标状态
     * @return 是否转换成功
     */
    public static boolean transitionOrRestart(UserChrome userChrome, UserChrome.BrowserStatus targetStatus) {
        UserChrome.BrowserStatus currentStatus = userChrome.getStatus();
        
        // 如果可以直接转换，直接转换
        if (canTransition(currentStatus, targetStatus)) {
            transition(userChrome, targetStatus);
            return true;
        }
        
        // 如果可以从CLOSED状态转换到目标状态，先转换到CLOSED
        if (canTransition(UserChrome.BrowserStatus.CLOSED, targetStatus)) {
            if (canTransition(currentStatus, UserChrome.BrowserStatus.CLOSED)) {
                transition(userChrome, UserChrome.BrowserStatus.CLOSED, "准备重启");
                transition(userChrome, targetStatus, "重启后转换");
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 记录状态转换日志
     * 
     * @param userId 用户ID
     * @param from 源状态
     * @param to 目标状态
     * @param reason 转换原因
     */
    private static void logStateTransition(String userId, UserChrome.BrowserStatus from, 
                                          UserChrome.BrowserStatus to, String reason) {
        log.info("状态转换: userId={}, {} -> {}, reason={}", userId, from, to, reason != null ? reason : "无");
        
        // 记录到历史记录（最多保留最近50条）
        stateHistory.computeIfAbsent(userId, k -> new ArrayList<>())
            .add(new StateTransition(from, to, reason != null ? reason : "无"));
        
        List<StateTransition> history = stateHistory.get(userId);
        if (history.size() > 50) {
            history.remove(0);  // 移除最旧的记录
        }
    }
    
    /**
     * 触发状态变化事件
     * 可以在这里添加事件监听器机制，通知其他组件状态变化
     * 
     * @param userChrome 浏览器实例
     * @param from 源状态
     * @param to 目标状态
     */
    private static void fireStateChangeEvent(UserChrome userChrome, 
                                            UserChrome.BrowserStatus from, 
                                            UserChrome.BrowserStatus to) {
        // 记录状态变化事件
        log.debug("状态变化事件: userId={}, {} -> {}", userChrome.getUserId(), from, to);
        
        // 这里可以添加事件发布机制，例如：
        // eventPublisher.publish(new BrowserStateChangeEvent(userChrome, from, to));
    }
    
    /**
     * 获取状态转换历史
     * 
     * @param userId 用户ID
     * @return 状态转换历史列表
     */
    public static List<StateTransition> getStateHistory(String userId) {
        List<StateTransition> history = stateHistory.get(userId);
        return history != null ? new ArrayList<>(history) : new ArrayList<>();
    }
    
    /**
     * 清除状态转换历史
     * 
     * @param userId 用户ID
     */
    public static void clearStateHistory(String userId) {
        stateHistory.remove(userId);
    }
}
