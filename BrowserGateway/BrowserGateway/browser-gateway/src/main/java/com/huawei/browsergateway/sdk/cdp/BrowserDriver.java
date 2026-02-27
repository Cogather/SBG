package com.huawei.browsergateway.sdk.cdp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 浏览器驱动实现，封装Chrome API
 */
public class BrowserDriver {
    
    private static final Logger log = LoggerFactory.getLogger(BrowserDriver.class);
    
    private Type.Context context;
    private final DriverClient client;
    private final BrowserOptions options;
    
    /**
     * 构造函数
     * 
     * @param options 浏览器配置选项
     */
    public BrowserDriver(BrowserOptions options) {
        this.options = options;
        this.client = new ClientImpl(options.getEndpoint());
        
        log.info("创建BrowserDriver，端点: {}", options.getEndpoint());
        
        // 创建或查找浏览器
        Type.Browser browser = createOrFindBrowser();
        
        // 创建上下文
        Request.CreateContext createContextReq = new Request.CreateContext();
        createContextReq.setViewport(options.getViewport());
        createContextReq.setRecordData(options.getRecordData());
        
        this.context = client.context(browser.getId()).create(createContextReq);
        log.info("BrowserDriver创建成功，contextId={}", context.getId());
    }
    
    /**
     * 创建或查找浏览器实例
     */
    private Type.Browser createOrFindBrowser() {
        try {
            // 先尝试列出已有浏览器
            List<Type.Browser> browsers = client.browser().list();
            if (browsers != null && !browsers.isEmpty()) {
                // 查找匹配的浏览器类型
                for (Type.Browser browser : browsers) {
                    if (browser.getBrowserType() == options.getBrowserType()) {
                        log.info("找到已有浏览器实例: {}", browser.getId());
                        return browser;
                    }
                }
            }
            
            // 没有找到，创建新的浏览器
            Request.CreateBrowser createBrowserReq = new Request.CreateBrowser();
            createBrowserReq.setBrowserType(options.getBrowserType());
            createBrowserReq.setLimit(options.getLimit());
            
            Type.Browser browser = client.browser().create(createBrowserReq);
            log.info("创建新浏览器实例: {}", browser.getId());
            return browser;
        } catch (Exception e) {
            log.error("创建或查找浏览器失败", e);
            throw new RuntimeException("创建或查找浏览器失败", e);
        }
    }
    
    /**
     * 关闭浏览器驱动
     */
    public void close() {
        try {
            if (context != null) {
                // 保存用户数据
                saveUserdata();
                
                // 删除上下文
                client.context(context.getBrowserId()).delete(context.getId());
                log.info("BrowserDriver已关闭，contextId={}", context.getId());
            }
            
            // 关闭HTTP客户端
            if (client instanceof ClientImpl) {
                ((ClientImpl) client).close();
            }
        } catch (Exception e) {
            log.error("关闭BrowserDriver异常", e);
        }
    }
    
    /**
     * 保存用户数据
     */
    public void saveUserdata() {
        if (context != null) {
            try {
                client.context(context.getBrowserId()).saveUserdata(context.getId());
                log.debug("用户数据已保存，contextId={}", context.getId());
            } catch (Exception e) {
                log.error("保存用户数据失败", e);
            }
        }
    }
    
    /**
     * 创建新页面
     * 
     * @param url 页面URL
     * @return 页面ID
     */
    public String newPage(String url) {
        if (context == null) {
            throw new IllegalStateException("上下文未初始化");
        }
        
        try {
            Type.Page page = client.context(context.getBrowserId()).page(context.getId()).create(url);
            log.info("创建新页面: pageId={}, url={}", page.getId(), url);
            return page.getId();
        } catch (Exception e) {
            log.error("创建新页面失败: url={}", url, e);
            throw new RuntimeException("创建新页面失败", e);
        }
    }
    
    /**
     * 导航到指定URL
     * 
     * @param url 目标URL
     */
    public void gotoUrl(String url) {
        if (context == null) {
            throw new IllegalStateException("上下文未初始化");
        }
        
        try {
            client.context(context.getBrowserId()).page(context.getId()).gotoUrl(url);
            log.info("导航到URL: {}", url);
        } catch (Exception e) {
            log.error("导航到URL失败: url={}", url, e);
            throw new RuntimeException("导航到URL失败", e);
        }
    }
    
    /**
     * 执行JavaScript脚本
     * 
     * @param script JavaScript脚本
     * @return 执行结果
     */
    public Object executeScript(String script) {
        if (context == null) {
            throw new IllegalStateException("上下文未初始化");
        }
        
        try {
            Request.JSResult result = client.context(context.getBrowserId()).page(context.getId()).execute(script);
            log.debug("执行JavaScript成功: script={}", script);
            return result != null ? result.getValue() : null;
        } catch (Exception e) {
            log.error("执行JavaScript失败: script={}", script, e);
            throw new RuntimeException("执行JavaScript失败", e);
        }
    }
    
    /**
     * 执行CDP命令
     * 
     * @param method CDP方法名
     * @param param CDP参数
     * @return CDP响应
     */
    public Map<String, Object> executeCdp(String method, Map<String, Object> param) {
        if (context == null) {
            throw new IllegalStateException("上下文未初始化");
        }
        
        try {
            Map<String, Object> result = client.context(context.getBrowserId()).page(context.getId()).executeCdp(method, param);
            log.debug("执行CDP命令成功: method={}", method);
            return result;
        } catch (Exception e) {
            log.error("执行CDP命令失败: method={}", method, e);
            throw new RuntimeException("执行CDP命令失败", e);
        }
    }
    
    /**
     * 根据标签名查找元素
     * 
     * @param tagName 标签名
     * @return 元素ID
     */
    public String findElementByTagName(String tagName) {
        if (context == null) {
            throw new IllegalStateException("上下文未初始化");
        }
        
        try {
            String elementId = client.context(context.getBrowserId()).page(context.getId()).findElement("tag:" + tagName);
            log.debug("查找元素成功: tagName={}, elementId={}", tagName, elementId);
            return elementId;
        } catch (Exception e) {
            log.error("查找元素失败: tagName={}", tagName, e);
            throw new RuntimeException("查找元素失败", e);
        }
    }
    
    /**
     * 获取上下文
     */
    public Type.Context getContext() {
        return context;
    }
    
    /**
     * 获取客户端
     */
    public DriverClient getClient() {
        return client;
    }
}
