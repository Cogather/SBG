package com.huawei.browsergateway.sdk;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 浏览器驱动实现类，用于管理浏览器实例和上下文
 * 提供浏览器自动化的高级API，包括导航、脚本执行和元素交互
 */
@Data
public class BrowserDriver {

    private Type.Context context;
    private final DriverClient client;
    private final DriverClient.Context contextCli;

    /**
     * 构造函数，使用指定的配置选项创建浏览器驱动
     * 自动查找或创建浏览器实例并初始化上下文
     *
     * @param options 浏览器配置选项
     */
    public BrowserDriver(BrowserOptions options) {
        this.client = new ClientImpl(options.getEndpoint());
        Type.Browser browser = findOrCreateBrowser(options);
        this.contextCli = client.context(browser.getId());
        this.context = createContext(options);
    }

    /**
     * 查找可用的浏览器实例，如果不存在则创建新实例
     *
     * @param options 浏览器配置选项
     * @return 浏览器实例
     */
    private Type.Browser findOrCreateBrowser(BrowserOptions options) {
        return client.browser().list()
                .stream()
                .filter(browser -> isBrowserAvailable(browser, options))
                .findFirst()
                .orElseGet(() -> createNewBrowser(options));
    }

    /**
     * 检查浏览器是否可用（基于类型和使用限制）
     *
     * @param browser 待检查的浏览器实例
     * @param options 浏览器配置选项
     * @return 如果浏览器可用返回true
     */
    private boolean isBrowserAvailable(Type.Browser browser, BrowserOptions options) {
        return Objects.equals(browser.getBrowserType(), options.getBrowserType())
                && browser.getUsed() < options.getLimit();
    }

    /**
     * 创建新的浏览器实例
     *
     * @param options 浏览器配置选项
     * @return 新创建的浏览器实例
     */
    private Type.Browser createNewBrowser(BrowserOptions options) {
        String uuid = UUID.randomUUID().toString(true);
        Request.CreateBrowser req = Request.CreateBrowser.from(options, uuid);
        return client.browser().create(req);
    }

    /**
     * 创建新的浏览器上下文
     *
     * @param options 浏览器配置选项
     * @return 新创建的上下文
     */
    private Type.Context createContext(BrowserOptions options) {
        Request.CreateContext createContextReq = Request.CreateContext.from(options);
        return contextCli.create(createContextReq);
    }

    /**
     * 关闭当前浏览器上下文
     */
    public void close() {
        client.context(context.getBrowserId()).delete(context.getId());
    }

    /**
     * 保存当前上下文的用户数据
     */
    public void saveUserdata() {
        client.context(context.getBrowserId()).saveUserdata(context.getId());
    }

    /**
     * 创建新页面并导航到指定URL
     *
     * @param url 页面URL
     * @return 页面ID
     */
    public String newPage(String url) {
        context = contextCli.page(context.getId()).create(url);
        return context.getCurrent();
    }

    /**
     * 导航到指定URL
     *
     * @param url 目标URL
     */
    public void gotoUrl(String url) {
        context = contextCli.page(context.getId()).gotoUrl(url);
    }

    /**
     * 在当前页面执行JavaScript代码并返回结果
     * 处理不同的结果类型，包括元素、字符串、数字和对象
     *
     * @param script 要执行的JavaScript代码
     * @return 执行结果（WebElement、String、Long、Map或null）
     */
    public Object executeScript(String script) {
        Request.JSResult jsResult = contextCli.page(context.getId()).execute(script);
        return parseJSResult(jsResult);
    }

    /**
     * 根据结果类型解析JavaScript执行结果
     *
     * @param jsResult JavaScript执行结果
     * @return 解析后的结果对象
     */
    private Object parseJSResult(Request.JSResult jsResult) {
        switch (jsResult.getResultType()) {
            case "element":
                return parseElementResult(jsResult);
            case "string":
                return jsResult.getValue();
            case "int":
                return Long.valueOf(jsResult.getValue());
            case "none":
                return null;
            case "dict":
                return parseDictResult(jsResult);
            default:
                throw new IllegalArgumentException("不支持的结果类型: " + jsResult.getResultType());
        }
    }

    /**
     * 解析JavaScript执行结果中的元素
     *
     * @param jsResult JavaScript执行结果
     * @return WebElement实例
     */
    private WebElement parseElementResult(Request.JSResult jsResult) {
        return WebElementImpl.parse(jsResult.getValue(), this);
    }

    /**
     * 解析JavaScript执行结果中的字典对象
     * 处理纯对象和包含WebElement的对象
     *
     * @param jsResult JavaScript执行结果
     * @return 包含解析值的Map
     */
    private Map<String, Object> parseDictResult(Request.JSResult jsResult) {
        if (jsResult.getElementKeys().isEmpty()) {
            return JSONUtil.toBean(jsResult.getValue(), new TypeReference<Map<String, Object>>() {}, true);
        }
        return parseDictWithElements(jsResult);
    }

    /**
     * 解析包含WebElement的字典对象
     *
     * @param jsResult JavaScript执行结果
     * @return 正确解析WebElement的Map
     */
    private Map<String, Object> parseDictWithElements(Request.JSResult jsResult) {
        Map<String, Object> result = new HashMap<>();
        JSONObject jsonObject = JSONUtil.parseObj(jsResult.getValue());

        jsonObject.forEach((key, value) -> {
            if (jsResult.getElementKeys().contains(key)) {
                result.put(key, WebElementImpl.parse(jsonObject.get(key).toString(), this));
            } else {
                result.put(key, value);
            }
        });
        return result;
    }

    /**
     * 在页面元素上执行操作
     *
     * @param action 要执行的元素操作
     */
    public void executeElement(Request.Action action) {
        contextCli.page(context.getId()).executeElement(action);
    }

    /**
     * 执行CDP（Chrome DevTools Protocol）命令
     *
     * @param method CDP方法名
     * @param param  CDP参数
     * @return CDP执行结果
     */
    public Map<String, Object> executeCdp(String method, Map<String, Object> param) {
        return contextCli.page(context.getId()).executeCdp(method, param);
    }

    /**
     * 关闭当前页面
     */
    public void closeCurrentPage() {
        context = contextCli.page(context.getId()).delete(context.getCurrent());
    }

    /**
     * 获取当前页面的URL
     *
     * @return 当前URL
     */
    public String getCurrentUrl() {
        this.context = contextCli.get(context.getId());
        return context.getCurrentUrl();
    }

    /**
     * 在浏览器历史记录中后退
     */
    public void back() {
        contextCli.page(context.getId()).goBack();
    }

    /**
     * 在浏览器历史记录中前进
     */
    public void forward() {
        contextCli.page(context.getId()).goForward();
    }

    /**
     * 根据标签名查找元素
     *
     * @param tagName HTML标签名
     * @return WebElement实例
     */
    public WebElement findElementByTagName(String tagName) {
        Request.JSResult jsResult = contextCli.page(context.getId()).findElement(tagName);
        return WebElementImpl.parse(jsResult.getValue(), this);
    }

    /**
     * 获取元素的尺寸
     *
     * @param elementId 元素ID
     * @return 元素尺寸
     */
    public Type.Size getSize(String elementId) {
        return contextCli.page(context.getId()).getElementSize(elementId);
    }
}