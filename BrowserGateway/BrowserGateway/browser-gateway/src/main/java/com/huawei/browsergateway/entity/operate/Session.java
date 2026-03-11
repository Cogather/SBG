package com.huawei.browsergateway.entity.operate;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 会话信息
 */
@Data
public class Session {
    @Alias("session_id")
    private String sessionId;
    @Alias("app_type")
    private Integer appType;
    @Alias("started_at")
    private String startedAt;
    @Alias("finished_at")
    private String finishedAt;
    @Alias("tcp_unique_id")
    private String TcpUniqueId;

    public Session(String sessionId, Integer appType, String startedAt, String finishedAt, String tcpUniqueId) {
        this.sessionId = sessionId;
        this.appType = appType;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.TcpUniqueId = tcpUniqueId;
    }
}
