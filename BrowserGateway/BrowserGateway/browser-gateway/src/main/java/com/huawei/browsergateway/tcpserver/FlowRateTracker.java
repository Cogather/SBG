package com.huawei.browsergateway.tcpserver;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 流量速率追踪器
 * 用于统计和追踪TCP连接的数据流量速率
 */
@Component
public class FlowRateTracker {
    private static final String KEY_SEPARATOR = "@";

    private final ConcurrentHashMap<String, AtomicLong> flowRateMap = new ConcurrentHashMap<>();

    /**
     * 添加数据流量
     *
     * @param sessionId 会话ID
     * @param serviceType 服务类型（control/media）
     * @param dataSize 数据大小（字节）
     */
    public void add(String sessionId, String serviceType, int dataSize) {
        String key = generateKey(sessionId, serviceType);
        flowRateMap.computeIfAbsent(key, k -> new AtomicLong(0))
                .addAndGet(dataSize);
    }

    /**
     * 统计并清除指定会话的流量数据
     *
     * @param sessionId 会话ID
     * @param serviceType 服务类型
     * @return 累计流量大小（字节）
     */
    public long flowRateStat(String sessionId, String serviceType) {
        String key = generateKey(sessionId, serviceType);
        AtomicLong flowRate = flowRateMap.remove(key);
        return flowRate != null ? flowRate.get() : 0L;
    }

    /**
     * 生成流量统计的唯一键
     *
     * @param sessionId 会话ID
     * @param serviceType 服务类型
     * @return 组合键
     */
    private static String generateKey(String sessionId, String serviceType) {
        return sessionId + KEY_SEPARATOR + serviceType;
    }
}
