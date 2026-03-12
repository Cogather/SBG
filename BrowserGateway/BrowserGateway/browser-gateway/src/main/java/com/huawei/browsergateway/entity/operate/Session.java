package com.huawei.browsergateway.entity.operate;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 会话信息
 */
@Data
public class Session {
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
    /** TCP唯一标识 */
    @Alias("tcp_unique_id")
    private String TcpUniqueId;

    /**
     * 全参数构造方法
     *
     * @param sessionId   会话ID
     * @param appType     应用类型
     * @param startedAt   开始时间
     * @param finishedAt  结束时间
     * @param tcpUniqueId TCP唯一标识
     */
    public Session(String sessionId, Integer appType, String startedAt, String finishedAt, String tcpUniqueId) {
        this.sessionId = sessionId;
        this.appType = appType;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.TcpUniqueId = tcpUniqueId;
    }
}
