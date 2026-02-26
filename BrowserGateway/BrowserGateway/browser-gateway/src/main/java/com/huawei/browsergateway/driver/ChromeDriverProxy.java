package com.huawei.browsergateway.driver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chrome浏览器驱动代理类
 * 用于包装和管理Chrome浏览器驱动实例
 * 根据Moon SDK架构，ChromeDriver由MuenDriver内部管理，此代理类提供统一的访问接口
 */
public class ChromeDriverProxy {
    
    private static final Logger log = LoggerFactory.getLogger(ChromeDriverProxy.class);
    
    /**
     * 实际的ChromeDriver实例
     * 可能由MuenDriver内部创建，或通过其他方式获取
     */
    private Object chromeDriver;
    
    /**
     * 用户ID
     */
    private final String userId;
    
    /**
     * 是否已关闭
     */
    private volatile boolean closed = false;
    
    /**
     * 构造函数
     * 
     * @param userId 用户ID
     * @param chromeDriver 实际的ChromeDriver实例（可能为null，由MuenDriver管理）
     */
    public ChromeDriverProxy(String userId, Object chromeDriver) {
        this.userId = userId;
        this.chromeDriver = chromeDriver;
        log.info("创建ChromeDriverProxy: userId={}, chromeDriver={}", userId, chromeDriver != null ? "已设置" : "null");
    }
    
    /**
     * 获取实际的ChromeDriver实例
     * 
     * @return ChromeDriver实例
     */
    public Object getChromeDriver() {
        return chromeDriver;
    }
    
    /**
     * 设置ChromeDriver实例
     * 
     * @param chromeDriver ChromeDriver实例
     */
    public void setChromeDriver(Object chromeDriver) {
        this.chromeDriver = chromeDriver;
        log.debug("设置ChromeDriver: userId={}", userId);
    }
    
    /**
     * 获取用户ID
     * 
     * @return 用户ID
     */
    public String getUserId() {
        return userId;
    }
    
    /**
     * 关闭浏览器驱动
     * 根据Moon SDK架构，ChromeDriver可能由MuenDriver管理，这里提供统一的关闭接口
     */
    public void quit() {
        if (closed) {
            log.debug("ChromeDriver已关闭，跳过: userId={}", userId);
            return;
        }
        
        log.info("关闭ChromeDriver: userId={}", userId);
        
        try {
            // 如果chromeDriver不为null，尝试调用quit方法
            if (chromeDriver != null) {
                // 使用反射调用quit方法（如果存在）
                try {
                    java.lang.reflect.Method quitMethod = chromeDriver.getClass().getMethod("quit");
                    quitMethod.invoke(chromeDriver);
                    log.info("ChromeDriver.quit()调用成功: userId={}", userId);
                } catch (NoSuchMethodException e) {
                    log.debug("ChromeDriver没有quit方法，可能由MuenDriver管理: userId={}", userId);
                } catch (Exception e) {
                    log.warn("调用ChromeDriver.quit()失败: userId={}", userId, e);
                }
            } else {
                log.debug("ChromeDriver为null，可能由MuenDriver管理: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("关闭ChromeDriver异常: userId={}", userId, e);
        } finally {
            closed = true;
            chromeDriver = null;
        }
    }
    
    /**
     * 检查是否已关闭
     * 
     * @return 是否已关闭
     */
    public boolean isClosed() {
        return closed;
    }
    
    /**
     * 检查ChromeDriver是否可用
     * 
     * @return 是否可用
     */
    public boolean isAvailable() {
        return !closed && chromeDriver != null;
    }
}
