package com.huawei.browsergateway.service;

import com.huawei.browsergateway.entity.event.EventInfo;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.entity.remote.UserBind;

import java.util.function.Consumer;

/**
 * 远程通信服务接口
 * 负责与外部系统的所有通信交互
 */
public interface IRemote {
    
    /**
     * 创建浏览器实例并建立连接
     * 
     * @param receivedControlPackets 控制数据包
     * @param parsedParams 解析后的参数
     * @param consumer 完成回调，可为null(预开场景)
     */
    void createChrome(byte[] receivedControlPackets, InitBrowserRequest parsedParams, Consumer<Object> consumer);
    
    /**
     * 获取用户绑定信息
     * 
     * @param sessionID 会话ID
     * @return UserBind 用户绑定信息
     */
    UserBind getUserBind(String sessionID);
    
    /**
     * 更新用户绑定信息
     * 
     * @param sessionID 会话ID
     * @return UserBind 更新后的用户绑定信息
     */
    UserBind updateUserBind(String sessionID);
    
    /**
     * 过期用户绑定信息
     * 
     * @param sessionID 会话ID
     */
    void expiredUserBind(String sessionID);
    
    /**
     * 处理浏览器事件
     * 
     * @param receivedControlPackets 控制数据包
     * @param userId 用户ID
     */
    void handleEvent(byte[] receivedControlPackets, String userId);
    
    /**
     * 回退页面
     * 
     * @param sessionID 会话ID
     */
    void fallback(String sessionID);
    
    /**
     * 错误回退
     * 
     * @param sessionId 会话ID
     */
    void fallbackByError(String sessionId);
    
    /**
     * 上报媒体流量统计
     * 
     * @param dataJson 统计数据JSON
     */
    void sendTrafficMedia(String dataJson);
    
    /**
     * 上报控制流量统计
     * 
     * @param dataJson 统计数据JSON
     */
    void sendTrafficControl(String dataJson);
    
    /**
     * 上报会话数据
     * 
     * @param dataJson 会话数据JSON
     */
    void sendSession(String dataJson);
    
    /**
     * 上报自定义事件
     * 
     * @param event 事件信息
     */
    <T> void reportEvent(EventInfo<T> event);
}
