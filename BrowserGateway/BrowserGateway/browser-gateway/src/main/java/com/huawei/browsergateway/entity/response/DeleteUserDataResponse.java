package com.huawei.browsergateway.entity.response;

import io.swagger.v3.oas.annotations.media.Schema;

public class DeleteUserDataResponse {
    @Schema(description = "imsi", example = "用户设备imsi号")
    private String imsi;

    @Schema(description = "imei", example = "用户设备imei号")
    private String imei;

    public String getImsi() {
        return imsi;
    }

    public String getImei() {
        return imei;
    }

    public DeleteUserDataResponse setImsi(String imsi) {
        this.imsi = imsi;
        return this;
    }

    public DeleteUserDataResponse setImei(String imei) {
        this.imei = imei;
        return this;
    }
}
