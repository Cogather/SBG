package com.huawei.browsergateway.sdk;

/**
 * Moon SDK上下文对象
 * 根据Moon-SDK应用模块分析文档，用于在MuenDriver方法调用间传递ChromeDriver实例
 */
public class HWContext {
    
    private Object chromeDriver; // ChromiumDriverProxy实例
    
    /**
     * 获取ChromeDriver实例
     * 
     * @return ChromeDriver实例
     */
    public Object getChromeDriver() {
        return chromeDriver;
    }
    
    /**
     * 设置ChromeDriver实例
     * 根据文档：将ChromiumDriver代理绑定到SDK上下文
     * 
     * @param chromeDriver ChromeDriver实例
     */
    public void setChromeDriver(Object chromeDriver) {
        this.chromeDriver = chromeDriver;
    }
}
