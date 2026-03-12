package com.huawei.browsergateway.entity.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 删除用户数据响应
 */
@Getter
public class DeleteUserDataResponse {
    @Schema(description = "imsi", example = "用户设备imsi号")
    private String imsi;

    @Schema(description = "imei", example = "用户设备imei号")
    private String imei;

    /**
     * 设置imsi（链式调用）
     *
     * @param imsi 用户设备imsi号
     * @return 当前对象
     */
    public DeleteUserDataResponse setImsi(String imsi) {
        this.imsi = imsi;
        return this;
    }

    /**
     * 设置imei（链式调用）
     *
     * @param imei 用户设备imei号
     * @return 当前对象
     */
    public DeleteUserDataResponse setImei(String imei) {
        this.imei = imei;
        return this;
    }
}
