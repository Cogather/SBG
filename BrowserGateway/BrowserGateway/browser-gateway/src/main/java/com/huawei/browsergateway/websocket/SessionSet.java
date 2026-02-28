package com.huawei.browsergateway.websocket;

import org.yeauty.pojo.Session;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionSet {
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public void addSession(String key, Session session) {
        Session oldSession = sessions.get(key);
        if (oldSession != null) {
            oldSession.close();
        }
        sessions.put(key, session);
    }

    public Session getSession(String key) {
        return sessions.get(key);
    }

    public void del(String key) {
        Session remove = sessions.remove(key);
        if (remove != null) {
            remove.close();
        }
    }

    public Set<String> allSessions() {
        return sessions.keySet();
    }
}
