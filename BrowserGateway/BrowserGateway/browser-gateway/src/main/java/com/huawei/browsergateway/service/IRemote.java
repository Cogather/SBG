package com.huawei.browsergateway.service;

import com.huawei.browsergateway.entity.event.EventInfo;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.service.impl.UserBind;

import java.util.function.Consumer;

/**
 * 远程服务交互接口，封装与 GIDS 服务的所有 HTTP 通信
 */
public interface IRemote {

    /**
     * 查询用户会话绑定信息
     */
    UserBind getUserBind(String sessionID);

    /**
     * 将用户会话绑定标记为过期
     */
    void expiredUserBind(String sessionID);

    /**
     * 创建浏览器实例并完成登录流程
     *
     * @param receivedControlPackets 控制流原始数据包
     * @param parsedParams           初始化请求参数
     * @param consumer               创建成功后的回调
     */
    void createChrome(byte[] receivedControlPackets, InitBrowserRequest parsedParams, Consumer<Object> consumer);

    /**
     * 正常退出：关闭应用并回退到空白页
     */
    void fallback(String sessionID);

    /**
     * 异常退出：关闭应用并标记页面控制错误状态
     */
    void fallbackByError(String sessionId);

    /**
     * 处理控制流事件，转发给 MuenDriver
     */
    void handleEvent(byte[] receivedControlPackets, String userId);

    /**
     * 更新用户会话绑定信息到远程，返回绑定对象
     */
    UserBind updateUserBind(String key);

    /**
     * 上报媒体流量统计数据
     */
    void sendTrafficMedia(String dataJson);

    /**
     * 上报控制流量统计数据
     */
    void sendTrafficControl(String dataJson);

    /**
     * 上报会话统计数据
     */
    void sendSession(String dataJson);

    /**
     * 上报服务端事件
     */
    <T> void reportEvent(EventInfo<T> event);
}
