package com.huawei.browsergateway.entity.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.sdk.BrowserOptions;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 初始化浏览器请求
 */
@Data
public class InitBrowserRequest {
    @Schema(description = "厂商", example = "xxx厂商")
    @JsonProperty("factory")
    private String factory;

    @Schema(description = "机型", example = "xx机型")
    @JsonProperty("dev_type")
    private String devType;

    @Schema(description = "扩展机型", example = "default")
    @JsonProperty("ext_type")
    private String extType;

    @Schema(description = "平台", example = "default")
    @JsonProperty("plat_type")
    private int platType;

    @Schema(description = "屏幕宽度", example = "240")
    @JsonProperty("lcd_width")
    private int lcdWidth;

    @Schema(description = "屏幕高度", example = "320")
    @JsonProperty("lcd_height")
    private int lcdHeight;

    @Schema(description = "appType")
    @JsonProperty("app_type")
    private int appType;

    @Schema(description = "appID")
    @JsonProperty("appid")
    private int appID;

    @Schema(description = "访问data服务请求的地址", example = "127.0.0.1:38080")
    private String innerMediaEndpoint = "127.0.0.1:30002";

    @Schema(description = "imsi", example = "用户设备imsi号")
    @JsonProperty("imsi")
    private String imsi;

    @Schema(description = "imei", example = "用户设备imei号")
    @JsonProperty("imei")
    private String imei;

    @Schema(description = "机器类型：1 按键，2 触屏", example = "1")
    @JsonProperty("device_type")
    private int deviceType;

    @Schema(description = "客户端语言", example = "en")
    @JsonProperty("client_language")
    private String clientLanguage;

    @Schema(description = "播放模式（0 JPG模式,1 实时流模式 默认）", example = "0")
    @JsonProperty("play_mode")
    private int playMode = 1;

    public BrowserOptions buildBrowserOptions(String userdata, Config config) {
        BrowserOptions options = new BrowserOptions();
        options.setEndpoint(config.getChrome().getEndpoint());
        options.setBaseDataDir(config.getBaseDataPath());
        options.setExecutablePath(config.getChrome().getExecutablePath());
        // 处理可能为null的扩展路径
        String extensionPath = config.getRecordExtensionPath();
        if (extensionPath != null) {
            options.setExtensionPaths(new ArrayList<>(List.of(extensionPath)));
        } else {
            options.setExtensionPaths(new ArrayList<>());
        }
        // 处理可能为null的扩展ID
        String extensionId = config.getChrome().getRecordExtensionId();
        if (extensionId != null) {
            options.setExtensionIds(new ArrayList<>(List.of(extensionId)));
            options.setAllowlistedExtensionId(extensionId);
        } else {
            options.setExtensionIds(new ArrayList<>());
        }
        options.setUserdata(userdata);
        options.setHeadless(config.getChrome().isHeadless());
        options.setLanguage(this.clientLanguage);
        return  options;
    }
}
