package com.huawei.browsergateway.entity.request;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * 加载扩展请求
 */
@Data
public class LoadExtensionRequest {
    @Schema(description = "bucket名称")
    @JsonProperty("bucket_name")
    private String bucketName;

    @Schema(description = "扩展文件路径")
    @JsonProperty("extension_file_path")
    private String extensionFilePath;

    @Schema(description = "插件名字")
    @JsonProperty("name")
    private String name;

    @Schema(description = "插件版本")
    @JsonProperty("version")
    private String version;

    @Schema(description = "插件类型")
    @JsonProperty("type")
    private String type;
}
