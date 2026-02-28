package com.huawei.browsergateway.tcpserver;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FlowRateTracker {
    private final ConcurrentHashMap<String, AtomicLong> flowRateMap = new ConcurrentHashMap<>();

    public void add(String sessionId, String serviceType, int dataSize) {
        this.flowRateMap.computeIfAbsent(generateKey(sessionId, serviceType), (k) -> new AtomicLong(0))
                .addAndGet(dataSize);
    }

    public long flowRateStat(String sessionId, String serviceType) {
        String key = generateKey(sessionId, serviceType);
        long result = this.flowRateMap.getOrDefault(key, new AtomicLong(0)).get();
        this.flowRateMap.remove(key);
        return result;
    }

    private static String generateKey(String sessionId, String serviceType) {
        return sessionId + "@" + serviceType;
    }
}