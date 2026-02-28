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
 * 浏览器驱动实现，封装Chrome API
 */
@Data
public class BrowserDriver {

    private Type.Context context;
    private final DriverClient client;
    private final DriverClient.Context contextCli;

    public BrowserDriver(BrowserOptions options) {
        this.client = new ClientImpl(options.getEndpoint());
        // 查询是否有浏览器
        Type.Browser browser = client.browser().list().
                stream().filter(f -> Objects.equals(f.getBrowserType(), options.getBrowserType())
                        && f.getUsed() < options.getLimit()).
                findFirst().
                orElseGet(() -> {
                    // 没有浏览器，则创建
                    String uuid = UUID.randomUUID().toString(true);
                    Request.CreateBrowser req = Request.CreateBrowser.from(options, uuid);
                    return client.browser().create(req);
                });

        this.contextCli = client.context(browser.getId());
        Request.CreateContext createContextReq = Request.CreateContext.from(options);
        this.context = contextCli.create(createContextReq);
    }


    public void close() {
        client.context(context.getBrowserId()).delete(context.getId());
    }

    public void saveUserdata() {
        client.context(context.getBrowserId()).saveUserdata(context.getId());
    }

    public String newPage(String url) {
        context = contextCli.page(context.getId()).create(url);
        Type.Page currentPage = context.getCurrentPage();
        return currentPage != null ? currentPage.getId() : null;
    }

    public void gotoUrl(String url) {
        context = contextCli.page(context.getId()).gotoUrl(url);
    }

    public Object executeScript(String script) {
        Request.JSResult jsResult = contextCli.page(context.getId()).execute(script);
        switch (jsResult.getResultType()) {
            case "element":
                return WebElementImpl.parse(jsResult.getValue(), this);
            case "string":
                return jsResult.getValue();
            case "int":
                return Long.valueOf(jsResult.getValue());
            case "none":
                return null;
            case "dict":
                if (jsResult.getElementKeys().isEmpty()) {
                    return JSONUtil.<Map<String, Object>>toBean(jsResult.getValue(), new TypeReference<>() {
                    }, true);
                }
                Map<String, Object> map = new HashMap<>();
                JSONObject jsonObject = JSONUtil.parseObj(jsResult.getValue());

                jsonObject.forEach((k,v)-> {
                    if (!jsResult.getElementKeys().contains(k)) {
                        map.put(k, v);
                        return;
                    }
                    map.put(k, WebElementImpl.parse(jsonObject.get(k).toString(),this));
                });
                return map;

            default:
                throw new IllegalArgumentException("result type is not support");
        }
    }

    public void executeElement(Request.Action action) {
        contextCli.page(context.getId()).executeElement(action);
    }

    public Map<String, Object> executeCdp(String method, Map<String, Object> param) {
        return contextCli.page(context.getId()).executeCdp(method, param);
    }

    public void closeCurrentPage() {
        Type.Page currentPage = context.getCurrentPage();
        if (currentPage != null) {
            context = contextCli.page(context.getId()).delete(currentPage.getId());
        }
    }

    public String getCurrentUrl() {
        this.context = contextCli.get(context.getId());
        return context.getCurrentUrl();
    }

    public void back() {
        contextCli.page(context.getId()).goBack();
    }

    public void forward() {
        contextCli.page(context.getId()).goForward();
    }

    public WebElement findElementByTagName(String tagName) {
        Request.JSResult jsResult = contextCli.page(context.getId()).findElement(tagName);
        return WebElementImpl.parse(jsResult.getValue(), this);
    }

    public Type.Size getSize(String elementId) {
        return contextCli.page(context.getId()).getElementSize(elementId);
    }
}