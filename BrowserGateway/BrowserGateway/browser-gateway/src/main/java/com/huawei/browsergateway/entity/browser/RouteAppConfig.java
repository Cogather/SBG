package com.huawei.browsergateway.entity.browser;

import lombok.Data;

/**
 * 路由应用配置
 */
@Data
public class RouteAppConfig {
    /** 厂商 */
    private String manufacturer;
    /** 机型 */
    private String model;
    /** 应用类型 */
    private int type;
    /** 路由模式 */
    private int mode;
    /** 扩展机型 */
    private String extendModel;
    /** 应用名称 */
    private String name;
    /** 应用描述 */
    private String description;
}
