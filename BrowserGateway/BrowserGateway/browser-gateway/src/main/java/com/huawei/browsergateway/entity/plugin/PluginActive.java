package com.huawei.browsergateway.entity.plugin;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 插件激活状态
 */
@Data
public class PluginActive {
    /** 插件名称 */
    @Alias("name")
    private String name;
    /** 插件版本 */
    @Alias("version")
    private String version;
    /** 插件类型 */
    @Alias("type")
    private String type;
    /** 插件状态 */
    @Alias("status")
    private String status;
    /** 存储桶名称 */
    @Alias("bucket")
    private String bucketName;
    /** 安装包名称 */
    @Alias("packageName")
    private String packageName;
}
