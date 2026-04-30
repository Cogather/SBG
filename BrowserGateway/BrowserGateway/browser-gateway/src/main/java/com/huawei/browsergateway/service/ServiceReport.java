package com.huawei.browsergateway.service;

import com.huawei.browsergateway.config.ReportConfig;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 服务实例状态上报数据，序列化后写入 CSE 实例属性
 *
 * @since 2026-04-16
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

    /** 本周期内累计音视频有效载荷平均带宽（Mbps），供 License / GIDS；与 {@link #used}、{@link #cap} 等字段相同，不使用 Hutool {@code @Alias} */
    private Integer tpUsed;

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