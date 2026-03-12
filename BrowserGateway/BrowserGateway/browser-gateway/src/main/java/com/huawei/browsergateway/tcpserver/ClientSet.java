package com.huawei.browsergateway.tcpserver;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端连接集合管理类
 * 管理所有活跃的TCP客户端连接
 */
public class ClientSet {
    protected final Map<String, Client> clientMap = new ConcurrentHashMap<>();

    /**
     * 添加或替换客户端连接
     * 如果已存在相同key的连接，会先关闭旧连接再添加新连接
     *
     * @param key 会话标识
     * @param client 客户端实例
     */
    public void set(String key, Client client) {
        Client existingClient = clientMap.get(key);
        if (existingClient != null) {
            existingClient.close();
        }
        clientMap.put(key, client);
    }

    /**
     * 根据key删除客户端连接
     *
     * @param key 会话标识
     */
    public void del(String key) {
        Client client = clientMap.remove(key);
        if (client != null) {
            client.close();
        }
    }

    /**
     * 删除指定的客户端连接
     *
     * @param client 客户端实例
     */
    public void del(Client client) {
        String sessionId = client.getStr(Client.VAL_SESSION_ID);
        if (sessionId != null) {
            del(sessionId);
        }
    }

    /**
     * 获取指定key的客户端连接
     *
     * @param key 会话标识
     * @return 客户端实例，不存在时返回null
     */
    public Client get(String key) {
        return clientMap.get(key);
    }

    /**
     * 获取所有客户端的会话标识集合
     *
     * @return 会话标识集合
     */
    public Set<String> allClient() {
        return clientMap.keySet();
    }
}
