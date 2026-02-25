package com.huawei.browsergateway.entity.response;

import lombok.Data;

/**
 * 插件信息响应
 */
@Data
public class PluginInfoResponse {
    private String name;
    private String version;
    private String type;
    private String status;
    private String bucketName;
    private String packageName;
    private String loadTime;
}
