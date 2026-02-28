package com.huawei.browsergateway.service.impl;

import cn.hutool.core.date.DateTime;
import lombok.Data;

@Data
public class UserBind {
    private String sessionId;
    private String token;
    private String browserInstance;
    private String mediaEndpoint;
    private String controlEndpoint;
    private String mediaTlsEndpoint;
    private String controlTlsEndpoint;
    private String innerMediaEndpoint;
    private String innerBrowserEndpoint;
    private DateTime heartbeats;
}
