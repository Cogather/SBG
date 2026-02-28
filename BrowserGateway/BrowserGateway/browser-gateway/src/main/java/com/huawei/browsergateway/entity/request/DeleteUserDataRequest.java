package com.huawei.browsergateway.entity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 删除用户数据请求
 */
@Data
public class DeleteUserDataRequest {
    @Schema(description = "imsi", example = "用户设备imsi号")
    private String imei;
    @Schema(description = "imei", example = "用户设备imei")
    private String imsi;
}
