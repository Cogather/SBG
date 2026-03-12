package com.huawei.browsergateway.service;

import cn.hutool.core.annotation.Alias;
import com.huawei.browsergateway.config.ReportConfig;
import lombok.Data;

/**
 * 服务实例状态上报数据，序列化后写入 CSE 实例属性
 */
@Data
public class ServiceReport {

    /** 本机 ID，同时作为 browserGWInnerEndpoint */
    @Alias("browserGWInnerEndpoint")
    private String id;

    /** 控制流对外暴露地址 */
    @Alias("edgeControlExtendEndpoint")
    private String controlEndpoint;

    /** 媒体流对外暴露地址 */
    @Alias("edgeMediaExtendEndpoint")
    private String mediaEndpoint;

    /** 媒体流 TLS 对外暴露地址 */
    @Alias("edgeMediaTlsExtendEndpoint")
    private String mediaTlsEndpoint;

    /** 控制流 TLS 对外暴露地址 */
    @Alias("edgeControlTlsExtendEndpoint")
    private String controlTlsEndpoint;

    /** 媒体流内网地址 */
    private String edgeMediaInnerEndpoint;

    /** 最大容量 */
    private Integer cap;

    /** 当前已使用数量 */
    private Integer used;

    /** 插件加载状态 */
    private String pluginStatus;

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
