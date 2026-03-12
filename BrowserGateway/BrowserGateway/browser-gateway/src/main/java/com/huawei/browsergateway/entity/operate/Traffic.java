package com.huawei.browsergateway.entity.operate;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 流量统计
 */
@Data
public class Traffic {
    /** 会话ID */
    @Alias("session_id")
    private String sessionId;
    /** 应用类型 */
    @Alias("app_type")
    private Integer appType;
    /** 开始时间 */
    @Alias("started_at")
    private String startedAt;
    /** 结束时间 */
    @Alias("finished_at")
    private String finishedAt;
    /** 出流量字节数 */
    @Alias("out_bytes")
    private Long outBytes;
    /** 客户端IP */
    @Alias("ip")
    private String ip;

    /**
     * 全参数构造方法
     *
     * @param sessionId  会话ID
     * @param appType    应用类型
     * @param startedAt  开始时间
     * @param finishedAt 结束时间
     * @param outBytes   出流量字节数
     * @param ip         客户端IP
     */
    public Traffic(String sessionId, Integer appType, String startedAt, String finishedAt, Long outBytes, String ip) {
        this.sessionId = sessionId;
        this.appType = appType;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.outBytes = outBytes;
        this.ip = ip;
    }
}
