package com.huawei.browsergateway.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 加载扩展响应
 */
@Data
public class LoadExtensionResponse {
    @Schema(description = "bucket名称")
    @JsonProperty("bucket_name")
    private String bucketName;

    @Schema(description = "扩展文件路径")
    @JsonProperty("extension_file_path")
    private String extensionFilePath;

    public LoadExtensionResponse setBucketName(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    public LoadExtensionResponse setExtensionFilePath(String extensionFilePath) {
        this.extensionFilePath = extensionFilePath;
        return this;
    }
}
