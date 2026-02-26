package com.huawei.browsergateway.entity.browser;

/**
 * 媒体状态枚举
 * 用于跟踪浏览器媒体流的状态
 */
public enum MediaState {
    /**
     * 空闲状态
     * 媒体流未启动
     */
    IDLE,
    
    /**
     * 流式传输中
     * 正在传输媒体流数据
     */
    STREAMING,
    
    /**
     * 录制中
     * 正在录制媒体流
     */
    RECORDING,
    
    /**
     * 暂停
     * 媒体流已暂停
     */
    PAUSED,
    
    /**
     * 错误状态
     * 媒体流出现错误
     */
    ERROR
}
