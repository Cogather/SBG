package com.huawei.browsergateway.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 实例级「License 口径」音视频payload + TCP/IP头带宽累加器：统计 {@code VideoResponse#videoData}
 * 与 {@code AudioResponse#audioData} 长度之和，每个报文额外增加54字节(包含以太网帧头14字节 + IP头20字节 + TCP头20字节)
 * （在 {@link com.huawei.browsergateway.util.encode.TlvEncoder} 中写入）。
 * 与 {@link com.huawei.browsergateway.tcpserver.DataSizeTracker}（全量 TLV、按用户 HTTP 上报）解耦。
 *
 * <p>统计单位为 Mbps（兆比特每秒），计算公式（使用1000进制）：Mbps = (bytes * 8) / (periodMs * 1000)
 *
 * @since 2026-04-14
 */
@Component
public class TpusedMediaAccumulator {

    private static final Logger log = LogManager.getLogger(TpusedMediaAccumulator.class);

    /** 上报周期（毫秒），默认 30 秒 */
    @Value("${browsergw.scheduled.report-period:30000}")
    private long periodMs;

    private final AtomicLong periodBytes = new AtomicLong(0);

    /**
     * 累加本上报周期内下发给 UE 的音视频有效载荷字节数。
     *
     * @param delta 增加量；小于等于 0 时忽略
     */
    public void addPayloadBytes(long delta) {
        if (delta > 0) {
            periodBytes.addAndGet(delta);
        }
    }

    /**
     * 当前周期内累计字节（供内部计算使用）。
     */
    public long getCurrentBytes() {
        return periodBytes.get();
    }

    /**
     * 获取当前周期内的平均带宽（Mbps）。
     * 计算公式：Mbps = (bytes * 8) / (periodMs * 1000)
     *
     * @return 平均带宽（Mbps），取整
     */
    public int getCurrentMbps() {
        long bytes = periodBytes.get();
        if (bytes <= 0 || periodMs <= 0) {
            return 0;
        }
        double mbps = (new BigDecimal(bytes).multiply(new BigDecimal(8.0)).doubleValue()) / (periodMs * 1000.0);
        int result = (int) Math.round(mbps);
        log.debug("calculate media bandwidth, bytes: {}, periodMs: {}, mbps: {}", bytes, periodMs, result);
        return result;
    }

    /**
     * 重置周期计数器。无论上报成功或失败，每个周期结束后都应重置，
     * 以保证带宽统计的准确性（避免数据累积导致的瞬时峰值）。
     */
    public void reset() {
        long oldBytes = periodBytes.getAndSet(0);
        log.debug("reset media accumulator, oldBytes: {}", oldBytes);
    }
}