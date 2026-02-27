package com.huawei.browsergateway.sdk.muen;

/**
 * Moon SDK回调接口
 * 根据Moon-SDK应用模块分析文档，用于SDK向系统回调事件和数据
 * 
 * 注意：
 * - 此接口是功能接口（提供功能方法），而非事件回调接口
 * - 事件回调接口在MuenDriver中定义（onControlTcpConnected等）
 * - 当前实现与Moon SDK文档一致，接口定义正确
 * - 所有方法已在HWCallbackImpl中实现
 */
public interface HWCallback {
    
    /**
     * 获取配置信息
     * 根据文档：通过HTTP请求获取远程配置并返回JSON
     * 
     * @param configUrl 配置URL
     * @return JSON格式的配置信息
     */
    String getConfig(String configUrl);
    
    /**
     * 发送二进制数据到控制端
     * 根据文档：检查TCP客户端连接并发送二进制数据
     * 
     * @param data 二进制数据
     * @return 是否发送成功
     */
    boolean send(byte[] data);
    
    /**
     * 提供WebSocket连接地址
     * 
     * @return WebSocket地址
     */
    String address();
    
    /**
     * 上报事件到监控系统
     * 根据文档：封装事件并通过上报工具提交监控数据
     * 
     * @param eventType 事件类型
     * @param eventData 事件数据
     */
    void log(String eventType, Object eventData);
    
    /**
     * 通过WebSocket发送文本消息
     * 根据文档：检查WebSocket会话并发送文本消息
     * 
     * @param message 文本消息
     * @return 是否发送成功
     */
    boolean sendMessageToWebscoket(String message);
    
    /**
     * 下载远程文件
     * 根据文档：解析文件路径，创建本地目录，下载远程文件
     * 
     * @param remotePath 远程文件路径
     * @param localPath 本地保存路径
     * @return 是否下载成功
     */
    boolean getFile(String remotePath, String localPath);
    
    /**
     * 上传文件到远程存储
     * 根据文档：生成远程URL，先检查后上传文件到存储服务
     * 
     * @param localPath 本地文件路径
     * @param remotePath 远程存储路径
     * @return 是否上传成功
     */
    boolean uploadFile(String localPath, String remotePath);
}
