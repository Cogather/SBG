package com.huawei.browsergateway.sdk.cdp;

import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.chromium.ChromiumDriverCommandExecutor;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.service.DriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Selenium WebDriver代理类，重写和扩展ChromiumDriver
 * 提供标准的WebDriver API，同时支持CDP功能
 */
public class ChromiumDriverProxy extends ChromiumDriver implements WebDriver.TargetLocator {
    
    private static final Logger log = LoggerFactory.getLogger(ChromiumDriverProxy.class);
    
    private final BrowserDriver browserDriver;
    private String proxyContextId;
    
    /**
     * 构造函数
     * 
     * @param browserDriver 浏览器驱动实例
     */
    public ChromiumDriverProxy(BrowserDriver browserDriver) {
        // 注意：ChromiumDriver需要CommandExecutor和Capabilities
        // 这里创建一个虚拟的CommandExecutor，因为实际命令通过BrowserDriver执行
        super(createDummyCommandExecutor(), createDefaultCapabilities(), "chromium");
        this.browserDriver = browserDriver;
        if (browserDriver != null && browserDriver.getContext() != null) {
            this.proxyContextId = browserDriver.getContext().getId();
        }
        log.info("创建ChromiumDriverProxy，contextId={}", proxyContextId);
    }
    
    /**
     * 创建虚拟的CommandExecutor
     * 实际命令通过BrowserDriver执行，这里只是为了满足ChromiumDriver构造函数要求
     */
    private static CommandExecutor createDummyCommandExecutor() {
        // 创建一个虚拟的CommandExecutor
        // 实际命令会通过BrowserDriver执行，不会使用这个executor
        // 直接返回一个简单的实现，避免HttpCommandExecutor构造函数签名问题
        return new CommandExecutor() {
            @Override
            public org.openqa.selenium.remote.Response execute(org.openqa.selenium.remote.Command command) {
                throw new UnsupportedOperationException("CommandExecutor not used in ChromiumDriverProxy");
            }
        };
    }
    
    /**
     * 创建默认的Capabilities
     */
    private static Capabilities createDefaultCapabilities() {
        // 这里返回一个基本的Capabilities
        // 实际实现可能需要根据CDP服务配置
        return new org.openqa.selenium.chrome.ChromeOptions();
    }
    
    // ========== WebDriver接口实现 ==========
    
    @Override
    public void get(String url) {
        log.debug("导航到URL: {}", url);
        if (browserDriver != null) {
            browserDriver.gotoUrl(url);
        } else {
            throw new IllegalStateException("BrowserDriver未初始化");
        }
    }
    
    @Override
    public String getCurrentUrl() {
        if (browserDriver != null && browserDriver.getContext() != null) {
            // 通过执行JavaScript获取当前URL
            try {
                Object result = browserDriver.executeScript("return window.location.href;");
                return result != null ? result.toString() : "";
            } catch (Exception e) {
                log.warn("获取当前URL失败", e);
                return "";
            }
        }
        return "";
    }
    
    @Override
    public String getTitle() {
        if (browserDriver != null) {
            try {
                Object result = browserDriver.executeScript("return document.title;");
                return result != null ? result.toString() : "";
            } catch (Exception e) {
                log.warn("获取页面标题失败", e);
                return "";
            }
        }
        return "";
    }
    
    @Override
    public String getPageSource() {
        if (browserDriver != null) {
            try {
                Object result = browserDriver.executeScript("return document.documentElement.outerHTML;");
                return result != null ? result.toString() : "";
            } catch (Exception e) {
                log.warn("获取页面源码失败", e);
                return "";
            }
        }
        return "";
    }
    
    @Override
    public void close() {
        log.info("关闭当前窗口");
        // ChromiumDriverProxy的close方法通常关闭当前窗口，而不是整个浏览器
        // 这里可以根据需要实现窗口关闭逻辑
    }
    
    @Override
    public void quit() {
        log.info("退出浏览器");
        if (browserDriver != null) {
            browserDriver.close();
        }
    }
    
    @Override
    public Set<String> getWindowHandles() {
        // 返回窗口句柄集合
        // 这里简化实现，返回当前窗口
        return Set.of("default");
    }
    
    @Override
    public String getWindowHandle() {
        return "default";
    }
    
    @Override
    public WebDriver.TargetLocator switchTo() {
        return this;
    }
    
    @Override
    public WebDriver.Navigation navigate() {
        return new NavigationProxy(browserDriver);
    }
    
    @Override
    public WebDriver.Options manage() {
        return new org.openqa.selenium.WebDriver.Options() {
            @Override
            public WebDriver.Timeouts timeouts() {
                return new org.openqa.selenium.WebDriver.Timeouts() {
                    @Override
                    public WebDriver.Timeouts implicitlyWait(java.time.Duration duration) {
                        return this;
                    }
                    
                    @Override
                    public WebDriver.Timeouts pageLoadTimeout(java.time.Duration duration) {
                        return this;
                    }
                    
                    @Override
                    public WebDriver.Timeouts scriptTimeout(java.time.Duration duration) {
                        return this;
                    }
                    
                    @Override
                    public WebDriver.Timeouts pageLoadTimeout(long time, TimeUnit unit) {
                        return pageLoadTimeout(java.time.Duration.ofMillis(unit.toMillis(time)));
                    }
                    
                    @Override
                    public WebDriver.Timeouts setScriptTimeout(long time, TimeUnit unit) {
                        return scriptTimeout(java.time.Duration.ofMillis(unit.toMillis(time)));
                    }
                    
                    @Override
                    public WebDriver.Timeouts implicitlyWait(long time, TimeUnit unit) {
                        return implicitlyWait(java.time.Duration.ofMillis(unit.toMillis(time)));
                    }
                };
            }
            
            @Override
            public Cookie getCookieNamed(String name) {
                // 简化实现，返回null表示cookie不存在
                // 实际实现可以通过BrowserDriver获取cookie
                return null;
            }
            
            @Override
            public Set<Cookie> getCookies() {
                // 简化实现，返回空集合
                // 实际实现可以通过BrowserDriver获取所有cookie
                return Set.of();
            }
            
            @Override
            public void deleteAllCookies() {
                // 简化实现，删除所有cookie
                // 实际实现可以通过BrowserDriver删除cookie
            }
            
            @Override
            public void deleteCookie(Cookie cookie) {
                // 简化实现，删除指定cookie
                // 实际实现可以通过BrowserDriver删除cookie
            }
            
            @Override
            public void deleteCookieNamed(String name) {
                // 简化实现，删除指定名称的cookie
                // 实际实现可以通过BrowserDriver删除cookie
            }
            
            @Override
            public void addCookie(Cookie cookie) {
                // 简化实现，添加cookie
                // 实际实现可以通过BrowserDriver添加cookie
                if (cookie != null && browserDriver != null) {
                    try {
                        // 通过JavaScript设置cookie
                        String cookieScript = String.format(
                            "document.cookie = '%s=%s; path=%s; domain=%s%s';",
                            cookie.getName(),
                            cookie.getValue(),
                            cookie.getPath() != null ? cookie.getPath() : "/",
                            cookie.getDomain() != null ? cookie.getDomain() : "",
                            cookie.isSecure() ? "; secure" : ""
                        );
                        browserDriver.executeScript(cookieScript);
                    } catch (Exception e) {
                        log.warn("添加cookie失败: {}", cookie, e);
                    }
                }
            }
            
            @Override
            public org.openqa.selenium.WebDriver.ImeHandler ime() {
                return new org.openqa.selenium.WebDriver.ImeHandler() {
                    @Override
                    public List<String> getAvailableEngines() {
                        return List.of();
                    }
                    
                    @Override
                    public String getActiveEngine() {
                        return "";
                    }
                    
                    @Override
                    public boolean isActivated() {
                        return false;
                    }
                    
                    @Override
                    public void deactivate() {
                        // 简化实现
                    }
                    
                    @Override
                    public void activateEngine(String engine) {
                        // 简化实现
                    }
                };
            }
            
            @Override
            public org.openqa.selenium.WebDriver.Window window() {
                return new WindowProxy(browserDriver);
            }
            
            @Override
            public org.openqa.selenium.logging.Logs logs() {
                return new org.openqa.selenium.logging.Logs() {
                    @Override
                    public org.openqa.selenium.logging.LogEntries get(String logType) {
                        return new org.openqa.selenium.logging.LogEntries(java.util.Collections.emptyList());
                    }
                    
                    @Override
                    public Set<String> getAvailableLogTypes() {
                        return Set.of();
                    }
                };
            }
        };
    }
    
    // ========== WebElement查找方法 ==========
    
    @Override
    public WebElement findElement(By by) {
        if (browserDriver == null) {
            throw new IllegalStateException("BrowserDriver未初始化");
        }
        
        try {
            String selector = convertByToSelector(by);
            String elementId = browserDriver.findElementByTagName(selector);
            if (elementId != null) {
                return new WebElementImpl(elementId, browserDriver);
            }
            throw new org.openqa.selenium.NoSuchElementException("未找到元素: " + by);
        } catch (Exception e) {
            log.error("查找元素失败: {}", by, e);
            throw new org.openqa.selenium.NoSuchElementException("查找元素失败: " + by, e);
        }
    }
    
    @Override
    public List<WebElement> findElements(By by) {
        // 简化实现，返回单个元素列表
        try {
            WebElement element = findElement(by);
            return List.of(element);
        } catch (org.openqa.selenium.NoSuchElementException e) {
            return List.of();
        }
    }
    
    /**
     * 将By对象转换为选择器字符串
     */
    private String convertByToSelector(By by) {
        // 简化实现，实际需要根据By类型转换
        return by.toString().replace("By.", "");
    }
    
    // ========== JavaScript执行 ==========
    
    @Override
    public Object executeScript(String script, Object... args) {
        if (browserDriver == null) {
            throw new IllegalStateException("BrowserDriver未初始化");
        }
        
        try {
            // 构建完整的脚本，包含参数
            String fullScript = buildScriptWithArgs(script, args);
            return browserDriver.executeScript(fullScript);
        } catch (Exception e) {
            log.error("执行JavaScript失败: script={}", script, e);
            throw new RuntimeException("执行JavaScript失败", e);
        }
    }
    
    @Override
    public Object executeAsyncScript(String script, Object... args) {
        // 异步脚本执行，简化实现
        return executeScript(script, args);
    }
    
    /**
     * 构建包含参数的脚本
     */
    private String buildScriptWithArgs(String script, Object... args) {
        if (args == null || args.length == 0) {
            return script;
        }
        
        // 简化实现，实际需要更复杂的参数处理
        StringBuilder sb = new StringBuilder(script);
        sb.append("; arguments = [");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(args[i]);
        }
        sb.append("];");
        return sb.toString();
    }
    
    // ========== Selenium风格API ==========
    
    /**
     * 导航到指定URL（Selenium风格）
     */
    public void navigate(String url) {
        get(url);
    }
    
    /**
     * 后退
     */
    public void back() {
        if (browserDriver != null) {
            browserDriver.executeScript("window.history.back();");
        }
    }
    
    /**
     * 前进
     */
    public void forward() {
        if (browserDriver != null) {
            browserDriver.executeScript("window.history.forward();");
        }
    }
    
    /**
     * 刷新
     */
    public void refresh() {
        if (browserDriver != null) {
            browserDriver.executeScript("window.location.reload();");
        }
    }
    
    // ========== TargetLocator接口实现 ==========
    
    @Override
    public WebDriver frame(int index) {
        return this;
    }
    
    @Override
    public WebDriver frame(String nameOrId) {
        return this;
    }
    
    @Override
    public WebDriver frame(WebElement frameElement) {
        return this;
    }
    
    @Override
    public WebDriver parentFrame() {
        return this;
    }
    
    @Override
    public WebDriver window(String nameOrHandle) {
        return this;
    }
    
    @Override
    public WebDriver defaultContent() {
        return this;
    }
    
    @Override
    public WebElement activeElement() {
        return findElement(By.tagName("body"));
    }
    
    @Override
    public WebDriver newWindow(WindowType typeHint) {
        log.debug("创建新窗口: type={}", typeHint);
        // 简化实现，返回当前驱动实例
        return this;
    }
    
    @Override
    public org.openqa.selenium.Alert alert() {
        return new org.openqa.selenium.Alert() {
            @Override
            public void dismiss() {
                browserDriver.executeScript("window.alert = function() {}; window.confirm = function() { return false; };");
            }
            
            @Override
            public void accept() {
                browserDriver.executeScript("window.alert = function() {}; window.confirm = function() { return true; };");
            }
            
            @Override
            public String getText() {
                return "";
            }
            
            @Override
            public void sendKeys(String keysToSend) {
                // 简化实现
            }
        };
    }
    
    // ========== 自定义方法 ==========
    
    /**
     * 获取代理上下文ID
     */
    public String getProxyContextId() {
        return proxyContextId;
    }
    
    /**
     * 设置代理上下文ID
     */
    public void setProxyContextId(String proxyContextId) {
        this.proxyContextId = proxyContextId;
    }
    
    /**
     * 保存用户数据
     */
    public void saveUserdata() {
        if (browserDriver != null) {
            browserDriver.saveUserdata();
        }
    }
    
    /**
     * 执行CDP命令
     */
    public java.util.Map<String, Object> executeCdp(String method, java.util.Map<String, Object> params) {
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
