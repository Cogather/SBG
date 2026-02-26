package com.huawei.browsergateway.entity.report;

import lombok.Data;

/**
 * 服务上报信息
 * 用于上报服务状态到CSE
 */
@Data
public class ServiceReport {
    /**
     * 服务ID（自身地址）
     */
    private String id;
    
    /**
     * 已使用的浏览器实例数量
     */
    private Integer used;
    
    /**
     * 媒体内部端点
     * 格式：address:port
     */
    private String mediaInnerEndpoint;
    
    /**
     * 插件状态
     */
    private String pluginStatus;
    
    /**
     * 容量上限
     */
    private Integer cap;
    
    /**
     * 控制端点
     */
    private String controlEndpoint;
    
    /**
     * 媒体端点
     */
    private String mediaEndpoint;
    
    /**
     * 信息TTL（秒）
     */
    private Integer ttl;
    
    /**
     * 构造函数
     * 
     * @param id 服务ID
     * @param mediaInnerEndpoint 媒体内部端点
     * @param pluginStatus 插件状态
     */
    public ServiceReport(String id, String mediaInnerEndpoint, String pluginStatus) {
        this.id = id;
        this.mediaInnerEndpoint = mediaInnerEndpoint;
        this.pluginStatus = pluginStatus;
    }
    
    /**
     * 完整构造函数
     * 
     * @param id 服务ID
     * @param used 已使用数量
     * @param mediaInnerEndpoint 媒体内部端点
     * @param pluginStatus 插件状态
     * @param cap 容量上限
     * @param controlEndpoint 控制端点
     * @param mediaEndpoint 媒体端点
     * @param ttl 信息TTL
     */
    public ServiceReport(String id, Integer used, String mediaInnerEndpoint, String pluginStatus,
                        Integer cap, String controlEndpoint, String mediaEndpoint, Integer ttl) {
        this.id = id;
        this.used = used;
        this.mediaInnerEndpoint = mediaInnerEndpoint;
        this.pluginStatus = pluginStatus;
        this.cap = cap;
        this.controlEndpoint = controlEndpoint;
        this.mediaEndpoint = mediaEndpoint;
        this.ttl = ttl;
    }
}
