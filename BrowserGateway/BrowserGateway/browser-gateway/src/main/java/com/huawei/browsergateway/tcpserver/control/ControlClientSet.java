package com.huawei.browsergateway.tcpserver.control;

import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.tcpserver.Client;
import com.huawei.browsergateway.tcpserver.ClientSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 控制流客户端集合管理类
 * 管理所有TCP控制流连接，支持连接替换时的回退处理
 */
@Component
public class ControlClientSet extends ClientSet {
    private static final String FALLBACK_FLAG = "true";

    @Autowired
    private IRemote remote;

    /**
     * 添加或替换客户端连接
     * 如果存在旧连接，会标记为已回退并触发回退逻辑
     *
     * @param key 会话标识
     * @param client 新的客户端实例
     */
    @Override
    public void set(String key, Client client) {
        Client existingClient = clientMap.get(key);

        if (existingClient != null) {
            handleExistingClient(existingClient, key);
        }

        clientMap.put(key, client);
    }

    /**
     * 处理已存在的客户端连接
     * 标记为已回退，触发回退逻辑，然后关闭连接
     */
    private void handleExistingClient(Client existingClient, String sessionId) {
        existingClient.set(Client.HAS_BEEN_FALLBACK, FALLBACK_FLAG);
        remote.fallback(sessionId);
        super.del(sessionId);
    }
}
