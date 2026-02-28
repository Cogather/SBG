package com.huawei.browsergateway.entity.operate;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

@Data
public class Traffic {
    @Alias("session_id")
    private String sessionId;
    @Alias("app_type")
    private String appType;
    @Alias("started_at")
    private String startedAt;
    @Alias("finished_at")
    private String finishedAt;
    @Alias("out_bytes")
    private Long outBytes;
    @Alias("ip")
    private String ip;

    public  Traffic(String sessionId, String appType, String startedAt, String finishedAt, Long outBytes, String ip) {
        this.sessionId = sessionId;
        this.appType = appType;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.outBytes = outBytes;
        this.ip = ip;
    }
}
