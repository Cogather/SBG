package com.huawei.browsergateway.sdk.cdp;

import lombok.Data;
import java.util.List;

/**
 * CDP服务数据类型定义
 * 包含Browser、Context、Page等核心数据模型
 */
public class Type {
    
    /**
     * 浏览器类型枚举
     */
    public enum BrowserType {
        KEYS(1),
        TOUCH(2);
        
        private final int value;
        
        BrowserType(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * 浏览器实例
     */
    @Data
    public static class Browser {
        private String id;
        private BrowserType browserType;
        private Integer used;
    }
    
    /**
     * 上下文实例
     */
    @Data
    public static class Context {
        private String id;
        private String current;
        private String browserId;
        private List<Page> pages;
    }
    
    /**
     * 页面实例
     */
    @Data
    public static class Page {
        private String id;
        private String url;
        private String browserId;
        private String contextId;
        private Boolean supportCdpSession;
    }
    
    /**
     * 健康检查结果
     */
    @Data
    public static class HealthCheckResult {
        private boolean success;
        private List<String> errContexts;
    }
}
