package com.huawei.browsergateway.service.impl;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 用户级别的锁管理器，为每个用户维护一把独立的 ReentrantLock，
 * 防止同一用户的并发操作产生竞态条件
 */
@Service
public class LockManager {

    private static final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * 获取指定用户的锁，不存在时自动创建
     */
    public ReentrantLock getLock(String userId) {
        return lockMap.computeIfAbsent(userId, k -> new ReentrantLock());
    }

    /**
     * 移除指定用户的锁（操作完成后调用，避免内存泄漏）
     */
    public void removeLock(String userId) {
        lockMap.remove(userId);
    }
}
