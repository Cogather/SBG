package com.huawei.browsergateway.service.impl;

import io.swagger.v3.oas.annotations.servers.Server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Server
public class LockManager {
    private static final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    public ReentrantLock getLock(String userId) {
        return lockMap.computeIfAbsent(userId, k -> new ReentrantLock());
    }
    public void removeLock(String userId) {
        lockMap.get(userId);
    }
}
