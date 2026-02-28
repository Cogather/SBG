package com.huawei.browsergateway.service;

import com.huawei.browsergateway.entity.event.EventInfo;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.service.impl.UserBind;

import java.util.function.Consumer;

public interface IRemote {
    UserBind getUserBind(String sessionID);

    void expiredUserBind(String sessionID);

    void createChrome(byte[] receivedControlPackets, InitBrowserRequest parsedParams, Consumer<Object> consumer);

    void fallback(String sessionID);

    void fallbackByError(String sessionId);

    void handleEvent(byte[] receivedControlPackets, String userId);

    UserBind updateUserBind(String key);

    void sendTrafficMedia(String dataJson);

    void sendTrafficControl(String dataJson);

    void sendSession(String dataJson);

    <T> void reportEvent(EventInfo<T> event);
}