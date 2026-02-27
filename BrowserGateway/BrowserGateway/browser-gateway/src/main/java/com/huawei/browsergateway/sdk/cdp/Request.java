package com.huawei.browsergateway.sdk.cdp;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * CDP服务请求数据模型
 */
public class Request {
    
    /**
     * 创建浏览器请求
     */
    @Data
    public static class CreateBrowser {
        private Type.BrowserType browserType;
        private Integer limit;
    }
    
    /**
     * 创建上下文请求
     */
    @Data
    public static class CreateContext {
        private String viewport;
        private String recordData;
    }
    
    /**
     * JavaScript执行结果
     */
    @Data
    public static class JSResult {
        private String resultType;
        private String value;
        private List<String> elementKeys;
    }
    
    /**
     * 动作请求
     */
    @Data
    public static class Action {
        private String type;
        private String elementId;
        private Map<String, Object> data;
    }
}
