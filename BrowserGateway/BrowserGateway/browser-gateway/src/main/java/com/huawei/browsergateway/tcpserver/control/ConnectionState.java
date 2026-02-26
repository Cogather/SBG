package com.huawei.browsergateway.tcpserver.control;

/**
 * 连接状态枚举
 */
public enum ConnectionState {
    /**
     * 未连接
     */
    DISCONNECTED,
    
    /**
     * 连接中
     */
    CONNECTING,
    
    /**
     * 已连接（未认证）
     */
    CONNECTED,
    
    /**
     * 已认证
     */
    AUTHENTICATED,
    
    /**
     * 认证失败
     */
    AUTH_FAILED,
    
    /**
     * 重连中
     */
    RECONNECTING,
    
    /**
     * 连接关闭
     */
    CLOSED
}
