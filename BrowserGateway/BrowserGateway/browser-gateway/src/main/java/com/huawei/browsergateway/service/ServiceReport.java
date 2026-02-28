package com.huawei.browsergateway.service;

import cn.hutool.core.annotation.Alias;
import com.huawei.browsergateway.config.ReportConfig;
import lombok.Data;

@Data
public class ServiceReport {
    @Alias("edgeControlExtendEndpoint")
    private String controlEndpoint; // 控制流地址 （对外暴露）
    @Alias("edgeMediaExtendEndpoint")
    private String mediaEndpoint; // 媒体流地址 （对外暴露）
    @Alias("edgeMediaTlsExtendEndpoint")
    private String mediaTlsEndpoint; // 控制流地址 （对外暴露）
    @Alias("edgeControlTlsExtendEndpoint")
    private String controlTlsEndpoint; // 控制流地址 （对外暴露）
    @Alias("browserGWInnerEndpoint")
    private String id; //本机ID
    private String edgeMediaInnerEndpoint;
    private Integer cap;
    private Integer used;
    private String pluginStatus;    // 插件加载状态

    public ServiceReport(String id, ReportConfig config, String mediaInnerEndpoint, String pluginStatus) {
        this.id = id;
        this.controlEndpoint = config.getControlEndpoint();
        this.mediaEndpoint = config.getMediaEndpoint();
        this.controlTlsEndpoint = config.getControlTlsEndpoint();
        this.mediaTlsEndpoint = config.getMediaTlsEndpoint();
        this.cap = config.getCap();
        this.edgeMediaInnerEndpoint = mediaInnerEndpoint;
        this.pluginStatus = pluginStatus;
    }
}
