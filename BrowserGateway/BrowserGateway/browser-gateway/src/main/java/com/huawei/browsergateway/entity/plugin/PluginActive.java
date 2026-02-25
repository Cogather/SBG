package com.huawei.browsergateway.entity.plugin;

import lombok.Data;

/**
 * 插件激活状态
 */
@Data
public class PluginActive {
    private String name;
    private String version;
    private String type;
    private String status;
    private String bucketName;
    private String packageName;
    private String loadTime;
}
