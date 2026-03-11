package com.huawei.browsergateway.websocket;

import org.yeauty.pojo.Session;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话集合管理类
 *
 * 提供线程安全的 WebSocket 会话管理功能，使用 ConcurrentHashMap 保证并发安全
 */
public class SessionSet {

    /** 会话存储容器，键为用户标识，值为 WebSocket 会话 */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * 添加会话，如果键已存在则关闭旧会话并替换
     *
     * @param key     会话标识键
     * @param session WebSocket 会话对象
     */
    public void addSession(String key, Session session) {
        Session oldSession = sessions.get(key);
        if (oldSession != null) {
            oldSession.close();
        }
        sessions.put(key, session);
    }

    /**
     * 根据键获取会话
     *
     * @param key 会话标识键
     * @return WebSocket 会话对象，不存在则返回 null
     */
    public Session getSession(String key) {
        return sessions.get(key);
    }

    /**
     * 删除会话并关闭连接
     *
     * @param key 会话标识键
     */
    public void del(String key) {
        Session remove = sessions.remove(key);
        if (remove != null) {
            remove.close();
        }
    }

    /**
     * 获取所有会话的键集合
     *
     * @return 会话键集合
     */
    public Set<String> allSessions() {
        return sessions.keySet();
    }
}

