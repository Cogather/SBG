package com.huawei.browsergateway.entity.request;

import lombok.Data;

/**
 * 初始化浏览器请求
 */
@Data
public class InitBrowserRequest {
    private String imei;
    private String imsi;
    private Integer lcdWidth;
    private Integer lcdHeight;
    private String appType;
    private String innerMediaEndpoint;
    private String factory;
    private String devType;
    private String extType;
    private Integer platType;
    private Integer appid;
    private Integer deviceType;
    private String clientLanguage;
    private Integer playMode;
}
