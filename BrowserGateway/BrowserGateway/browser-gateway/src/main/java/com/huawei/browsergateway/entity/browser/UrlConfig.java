package com.huawei.browsergateway.entity.browser;
import lombok.Data;

@Data
public class UrlConfig {
    private String nodeIdent;

    private int appType;

    private String url;

    private String appID;

    private String name;

    private boolean isVideoType;

    private boolean isWebType;

    private boolean isShortType;
}