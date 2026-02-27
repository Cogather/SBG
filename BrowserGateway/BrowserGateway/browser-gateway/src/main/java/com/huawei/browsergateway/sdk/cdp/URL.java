package com.huawei.browsergateway.sdk.cdp;

/**
 * CDP服务URL常量定义
 */
public class URL {
    
    /**
     * 浏览器相关端点
     */
    public static final String BROWSER = "/browser";
    public static final String BROWSER_LIST = "/browser/list";
    public static final String BROWSER_HEALTH = "/browser/health";
    
    /**
     * 上下文相关端点
     */
    public static final String CONTEXT = "/context";
    
    /**
     * 页面相关端点
     */
    public static final String PAGE = "/page";
    public static final String PAGE_GOTO = "/page/goto";
    public static final String PAGE_EXECUTE = "/page/execute";
    public static final String PAGE_CDP = "/page/cdp";
}
