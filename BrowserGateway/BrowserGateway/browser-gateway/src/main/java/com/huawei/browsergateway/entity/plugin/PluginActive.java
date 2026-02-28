package com.huawei.browsergateway.entity.plugin;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 插件激活状态
 */
@Data
public class PluginActive {
    @Alias("name")
    private String name;
    @Alias("version")
    private String version;
    @Alias("type")
    private String type;
    @Alias("status")
    private String status;
    @Alias("bucket")
    private String bucketName;
    @Alias("packageName")
    private String packageName;
}
