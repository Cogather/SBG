package com.huawei.browsergateway.service.impl;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockManager {
    private static final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();
    public ReentrantLock getLock(String userId) {
        return lockMap.computeIfAbsent(userId, k -> new ReentrantLock());
    }
    public void removeLock(String userId) {
        lockMap.remove(userId);
    }
}
