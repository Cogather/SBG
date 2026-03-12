package com.huawei.browsergateway.service;

import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.service.impl.UserChrome;

import java.util.Set;

/**
 * 浏览器会话集合管理接口，负责用户浏览器实例的生命周期管理
 */
public interface IChromeSet {

    /**
     * 为用户创建浏览器实例，若容量已满则抛出异常
     */
    UserChrome create(InitBrowserRequest request);

    /**
     * 获取指定用户的浏览器实例，不存在时返回 null
     */
    UserChrome get(String userId);

    /**
     * 关闭并删除用户浏览器实例，同时上传用户数据
     */
    void delete(String userId);

    /**
     * 为重启场景删除实例（不断开连接）
     */
    void deleteForRestart(String userId);

    /**
     * 关闭并删除所有用户浏览器实例
     */
    void deleteAll();

    /**
     * 获取当前所有在线用户 ID
     */
    Set<String> getAllUser();

    /**
     * 更新指定用户的心跳时间戳
     */
    void updateHeartbeats(String userId, long heartbeats);

    /**
     * 获取指定用户的心跳时间戳，用户不存在时返回 0
     */
    long getHeartbeats(String userId);

    /**
     * 上报当前已使用的浏览器实例数量到 CSE
     */
    void reportUsed();

    /**
     * 上报链路端点信息到 CSE
     *
     * @return 上报是否成功
     */
    boolean reportChainEndpoints();
}
