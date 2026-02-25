package com.huawei.browsergateway.entity.request;

import lombok.Data;

/**
 * 加载扩展请求
 */
@Data
public class LoadExtensionRequest {
    private String bucketName;
    private String extensionFilePath;
    private String name;
    private String version;
    private String type;
    private String packageName;
}
