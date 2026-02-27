package com.huawei.browsergateway.sdk.cdp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * DevTools代理
 * 提供Chrome DevTools Protocol命令执行能力
 */
public class DevToolsProxy {
    
    private static final Logger log = LoggerFactory.getLogger(DevToolsProxy.class);
    
    private final BrowserDriver browserDriver;
    
    public DevToolsProxy(BrowserDriver browserDriver) {
        this.browserDriver = browserDriver;
    }
    
    /**
     * 执行CDP命令
     * 
     * @param method CDP方法名
     * @param params CDP参数
     * @return CDP响应
     */
    public Map<String, Object> executeCdpCommand(String method, Map<String, Object> params) {
        log.debug("执行CDP命令: method={}", method);
        if (browserDriver != null) {
            return browserDriver.executeCdp(method, params);
        }
        throw new IllegalStateException("BrowserDriver未初始化");
    }
    
    /**
     * 获取BrowserDriver实例
     */
    public BrowserDriver getBrowserDriver() {
        return browserDriver;
    }
}
