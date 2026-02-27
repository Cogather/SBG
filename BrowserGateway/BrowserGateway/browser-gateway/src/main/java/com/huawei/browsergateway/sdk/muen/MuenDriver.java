package com.huawei.browsergateway.sdk.muen;

/**
 * Moon SDK驱动接口
 * 这是外部SDK的接口，用于与浏览器扩展通信
 * 根据Moon-SDK应用模块分析文档定义
 */
public interface MuenDriver {
    
    /**
     * 用户认证登录
     * 根据文档：接受二进制认证数据，返回JSON格式配置信息
     * 
     * @param loginData 登录数据包（TLV格式）
     * @return JSON格式的ChromeParams配置信息
     */
    String login(byte[] loginData);
    
    /**
     * 处理控制数据包
     * 根据文档：接受上下文和控制数据，处理浏览器控制消息
     * 
     * @param hwContext 上下文对象，包含ChromeDriver实例
     * @param data 控制数据包
     */
    void handle(HWContext hwContext, byte[] data);
    
    /**
     * 处理WebSocket消息
     * 根据文档：接收来自WebSocket的文本消息
     * 
     * @param message WebSocket文本消息
     */
    void receiveMessageFromWebscoket(String message);
    
    /**
     * 控制TCP连接建立回调
     */
    void onControlTcpConnected();
    
    /**
     * 控制TCP连接断开回调
     */
    void onControlTcpDisconnected();
    
    /**
     * 媒体TCP连接建立回调
     */
    void onMediaTcpConnected();
    
    /**
     * 媒体TCP连接断开回调
     */
    void onMediaTcpDisconnected();
}
