package com.huawei.browsergateway.entity.browser;

import lombok.Data;

/**
 * URL配置
 */
@Data
public class UrlConfig {
    /** 节点标识 */
    private String nodeIdent;

    /** 应用类型 */
    private int appType;

    /** 访问URL */
    private String url;

    /** 应用ID */
    private String appID;

    /** 应用名称 */
    private String name;

    /** 是否为视频类型 */
    private boolean isVideoType;

    /** 是否为Web类型 */
    private boolean isWebType;

    /** 是否为短视频类型 */
    private boolean isShortType;
}
