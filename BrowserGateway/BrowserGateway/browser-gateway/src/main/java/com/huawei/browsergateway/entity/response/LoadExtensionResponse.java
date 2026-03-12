package com.huawei.browsergateway.entity.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 加载扩展响应
 */
@Getter
public class LoadExtensionResponse {
    @Schema(description = "bucket名称")
    @JsonProperty("bucket_name")
    private String bucketName;

    @Schema(description = "扩展文件路径")
    @JsonProperty("extension_file_path")
    private String extensionFilePath;

    /**
     * 设置bucket名称（链式调用）
     *
     * @param bucketName bucket名称
     * @return 当前对象
     */
    public LoadExtensionResponse setBucketName(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    /**
     * 设置扩展文件路径（链式调用）
     *
     * @param extensionFilePath 扩展文件路径
     * @return 当前对象
     */
    public LoadExtensionResponse setExtensionFilePath(String extensionFilePath) {
        this.extensionFilePath = extensionFilePath;
        return this;
    }
}
