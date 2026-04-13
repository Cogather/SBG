package com.huawei.browsergateway.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 实例级「License 口径」音视频 payload 字节累加器：仅统计 {@code VideoResponse#videoData}
 * 与 {@code AudioResponse#audioData} 长度之和（在 {@link com.huawei.browsergateway.util.encode.TlvEncoder} 中写入）。
 * 与 {@link com.huawei.browsergateway.tcpserver.DataSizeTracker}（全量 TLV、按用户 HTTP 上报）解耦。
 */
@Component
public class TpusedMediaAccumulator {

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
     * 当前周期内累计字节（供上报 CSE 及测试使用）。
     */
    public long getCurrentBytes() {
        return periodBytes.get();
    }

    /**
     * CSE 写 {@code status} 成功后，从运行总量中扣除本次已上报的字节，保留上报期间新产生的增量。
     *
     * @param reportedBytes 本次 JSON 中携带的 {@code tpused}；小于等于 0 时忽略
     */
    public void subtractReported(long reportedBytes) {
        if (reportedBytes > 0) {
            periodBytes.addAndGet(-reportedBytes);
        }
    }
}
