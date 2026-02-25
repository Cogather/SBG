package com.huawei.browsergateway.entity.response;

import lombok.Data;

/**
 * 加载扩展响应
 */
@Data
public class LoadExtensionResponse {
    private String bucketName;
    private String extensionFilePath;
}
