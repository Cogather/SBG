package com.huawei.browsergateway.entity.browser;
import lombok.Data;

/**
 * 路由应用配置
 */
@Data
public class RouteAppConfig {
    private String manufacturer;
    private String model;
    private int type;
    private int mode;
    private String extendModel;
    private String name;
    private String description;
}