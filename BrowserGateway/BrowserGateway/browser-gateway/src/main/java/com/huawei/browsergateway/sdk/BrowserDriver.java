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
 * 浏览器驱动实现类
 * 封装Chrome API，提供浏览器操作的统一接口
 * 负责管理浏览器实例、上下文和页面的生命周期
 */
@Data
public class BrowserDriver {

    /** 当前上下文对象 */
    private Type.Context context;

    /** 驱动客户端实例 */
    private final DriverClient client;

    /** 上下文客户端实例 */
    private final DriverClient.Context contextClient;

    /**
     * 构造函数
     * 根据配置选项创建或获取浏览器实例，并创建上下文
     *
     * @param options 浏览器配置选项
     */
    public BrowserDriver(BrowserOptions options) {
        this.client = new ClientImpl(options.getEndpoint());

        // 查询是否有可用的浏览器实例
        Type.Browser browser = client.browser().list()
                .stream()
                .filter(b -> Objects.equals(b.getBrowserType(), options.getBrowserType())
                        && b.getUsed() < options.getLimit())
                .findFirst()
                .orElseGet(() -> {
                    // 没有可用浏览器，则创建新实例
                    String uuid = UUID.randomUUID().toString(true);
                    Request.CreateBrowser request = Request.CreateBrowser.from(options, uuid);
                    return client.browser().create(request);
                });

        this.contextClient = client.context(browser.getId());
        Request.CreateContext createContextRequest = Request.CreateContext.from(options);
        this.context = contextClient.create(createContextRequest);
    }

    /**
     * 关闭浏览器上下文
     */
    public void close() {
        client.context(context.getBrowserId()).delete(context.getId());
    }

    /**
     * 保存用户数据
     */
    public void saveUserdata() {
        client.context(context.getBrowserId()).saveUserdata(context.getId());
    }

    /**
     * 创建新页面
     *
     * @param url 页面URL
     * @return 新页面的ID
     */
    public String newPage(String url) {
        context = contextClient.page(context.getId()).create(url);
        Type.Page currentPage = context.getCurrentPage();
        return currentPage != null ? currentPage.getId() : null;
    }

    /**
     * 导航到指定URL
     *
     * @param url 目标URL
     */
    public void gotoUrl(String url) {
        context = contextClient.page(context.getId()).gotoUrl(url);
    }

    /**
     * 执行JavaScript脚本
     *
     * @param script JavaScript脚本
     * @return 执行结果（可能是字符串、数字、元素对象或Map）
     */
    public Object executeScript(String script) {
        Request.JSResult jsResult = contextClient.page(context.getId()).execute(script);
        String resultType = jsResult.getResultType();

        switch (resultType) {
            case "element":
                return WebElementImpl.parse(jsResult.getValue(), this);
            case "string":
                return jsResult.getValue();
            case "int":
                return Long.valueOf(jsResult.getValue());
            case "none":
                return null;
            case "dict":
                return parseDictResult(jsResult);
            default:
                throw new IllegalArgumentException("result type is not support: " + resultType);
        }
    }

    /**
     * 解析字典类型的结果
     *
     * @param jsResult JavaScript执行结果
     * @return 解析后的Map对象
     */
    private Object parseDictResult(Request.JSResult jsResult) {
        if (jsResult.getElementKeys().isEmpty()) {
            return JSONUtil.<Map<String, Object>>toBean(jsResult.getValue(), new TypeReference<>() {
            }, true);
        }

        Map<String, Object> resultMap = new HashMap<>();
        JSONObject jsonObject = JSONUtil.parseObj(jsResult.getValue());

        jsonObject.forEach((key, value) -> {
            if (!jsResult.getElementKeys().contains(key)) {
                resultMap.put(key, value);
            } else {
                resultMap.put(key, WebElementImpl.parse(jsonObject.get(key).toString(), this));
            }
        });

        return resultMap;
    }

    /**
     * 执行元素操作
     *
     * @param action 元素操作请求对象
     */
    public void executeElement(Request.Action action) {
        contextClient.page(context.getId()).executeElement(action);
    }

    /**
     * 执行CDP命令
     *
     * @param method CDP方法名
     * @param params CDP参数
     * @return CDP执行结果
     */
    public Map<String, Object> executeCdp(String method, Map<String, Object> params) {
        return contextClient.page(context.getId()).executeCdp(method, params);
    }

    /**
     * 关闭当前页面
     */
    public void closeCurrentPage() {
        Type.Page currentPage = context.getCurrentPage();
        if (currentPage != null) {
            context = contextClient.page(context.getId()).delete(currentPage.getId());
        }
    }

    /**
     * 获取当前页面URL
     *
     * @return 当前页面URL
     */
    public String getCurrentUrl() {
        this.context = contextClient.get(context.getId());
        return context.getCurrentUrl();
    }

    /**
     * 浏览器后退
     */
    public void back() {
        contextClient.page(context.getId()).goBack();
    }

    /**
     * 浏览器前进
     */
    public void forward() {
        contextClient.page(context.getId()).goForward();
    }

    /**
     * 根据标签名查找元素
     *
     * @param tagName 标签名
     * @return WebElement对象
     */
    public WebElement findElementByTagName(String tagName) {
        Request.JSResult jsResult = contextClient.page(context.getId()).findElement(tagName);
        return WebElementImpl.parse(jsResult.getValue(), this);
    }

    /**
     * 获取元素尺寸
     *
     * @param elementId 元素ID
     * @return 元素尺寸对象
     */
    public Type.Size getSize(String elementId) {
        return contextClient.page(context.getId()).getElementSize(elementId);
    }
}
