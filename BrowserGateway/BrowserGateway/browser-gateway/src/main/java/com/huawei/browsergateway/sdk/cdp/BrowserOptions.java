package com.huawei.browsergateway.sdk.cdp;

import lombok.Data;

/**
 * 浏览器配置选项
 */
@Data
public class BrowserOptions {
    
    /**
     * CDP服务端点URL
     */
    private String endpoint;
    
    /**
     * 浏览器类型
     */
    private Type.BrowserType browserType;
    
    /**
     * 视口大小
     */
    private String viewport;
    
    /**
     * 是否录制数据
     */
    private String recordData;
    
    /**
     * 浏览器实例限制
     */
    private Integer limit;
}
