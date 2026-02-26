package com.huawei.browsergateway.service;

import com.huawei.browsergateway.entity.browser.UserChrome;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;

import java.util.Set;

/**
 * 浏览器会话管理接口
 */
public interface IChromeSet {
    
    /**
     * 创建用户浏览器实例
     */
    UserChrome create(InitBrowserRequest request);
    
    /**
     * 获取用户浏览器实例
     */
    UserChrome get(String userId);
    
    /**
     * 删除用户浏览器实例
     */
    void delete(String userId);
    
    /**
     * 删除所有实例
     */
    void deleteAll();
    
    /**
     * 获取所有用户
     */
    Set<String> getAllUser();
    
    /**
     * 更新心跳
     */
    void updateHeartbeats(String userId, long heartbeats);
    
    /**
     * 获取心跳
     */
    long getHeartbeats(String userId);
    
    /**
     * 上报已使用数量
     * 上报当前使用的浏览器实例数量到CSE
     */
    void reportUsed();
    
    /**
     * 上报链路端点
     * 上报链路端点信息到CSE
     * 
     * @return 上报是否成功
     */
    boolean reportChainEndpoints();
}
