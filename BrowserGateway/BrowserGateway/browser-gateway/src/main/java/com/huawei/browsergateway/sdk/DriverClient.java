package com.huawei.browsergateway.sdk;

import cn.hutool.core.lang.TypeReference;

import java.util.List;
import java.util.Map;

/**
 * 驱动客户端接口定义
 * 提供浏览器、上下文、页面的管理接口
 */
public interface DriverClient {
    interface Browser {
        Type.Browser create(Request.CreateBrowser req);
        Type.Browser get(String id);
        List<Type.Browser> list();
        void delete(String id);
        Type.HealthCheckResult healthCheck();
    }
    interface Context {
        Type.Context create(Request.CreateContext req);
        Type.Context get(String id);
        List<Type.Context> list();
        void delete(String id);
        void saveUserdata(String contextId);
        Page page(String contextId);
    }
    interface Page {
        Type.Context delete(String id);
        Type.Context create(String url);
        Request.JSResult execute(String expression);
        Map<String, Object> executeCdp(String method, Map<String, Object> params);
        Type.Context gotoUrl(String url);
        void executeElement(Request.Action action);
        void goBack();
        void goForward();
        Request.JSResult findElement(String selector);
        Type.Size getElementSize(String elementId);
    }

    Browser browser();
    Context context(String browserId);
    <T> T request(String url, String method, String body, TypeReference<T> typeReference);
    void request(String url, String method);
}
