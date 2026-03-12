package com.huawei.browsergateway.service.impl;

import cn.hutool.core.date.DateTime;
import lombok.Data;

/**
 * 用户会话绑定数据，记录用户与浏览器实例及各端点的映射关系
 */
@Data
public class UserBind {

    private String sessionId;
    private String token;
    private String browserInstance;

    /** 控制流对外端点 */
    private String controlEndpoint;

    /** 媒体流对外端点 */
    private String mediaEndpoint;

    /** 媒体流 TLS 对外端点 */
    private String mediaTlsEndpoint;

    /** 控制流 TLS 对外端点 */
    private String controlTlsEndpoint;

    /** 媒体流内网端点 */
    private String innerMediaEndpoint;

    /** 浏览器网关内网端点 */
    private String innerBrowserEndpoint;

    /** 最近一次心跳时间 */
    private DateTime heartbeats;
}
