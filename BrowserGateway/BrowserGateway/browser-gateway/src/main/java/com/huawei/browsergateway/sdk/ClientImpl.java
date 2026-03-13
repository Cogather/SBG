package com.huawei.browsergateway.sdk;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.util.HttpUtil;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.openqa.selenium.json.Json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CDP服务通信客户端实现
 * 提供浏览器、上下文和页面管理功能
 */
public class ClientImpl implements DriverClient {
    private static final Json SELENIUM_JSON = new Json();
    private static final String API_BROWSERS_PATH = "/api/browsers";
    private static final String API_CONTEXTS_PATH = "/api/browsers/{}/contexts";
    private static final String API_PAGES_PATH = "/api/browsers/{}/contexts/{}/pages";

    /**
     * 浏览器管理实现
     */
    public static class BrowserImpl implements Browser {
        private final DriverClient client;

        public BrowserImpl(DriverClient client) {
            this.client = client;
        }

        @Override
        public Type.Browser create(Request.CreateBrowser req) {
            try {
                Type.Browser browser = client.request(
                        API_BROWSERS_PATH,
                        HttpPost.METHOD_NAME,
                        JSONUtil.toJsonStr(req),
                        new TypeReference<Type.Browser>() {}
                );

                if (browser == null) {
                    throw new RuntimeException("创建浏览器失败: 响应为空");
                }
                return browser;
            } catch (Exception e) {
                throw new RuntimeException("创建浏览器失败: " + e.getMessage(), e);
            }
        }

        @Override
        public Type.Browser get(String id) {
            return client.request(
                    StrUtil.format("{}/{}", API_BROWSERS_PATH, id),
                    HttpGet.METHOD_NAME,
                    null,
                    new TypeReference<Type.Browser>() {}
            );
        }

        @Override
        public List<Type.Browser> list() {
            return client.request(
                    API_BROWSERS_PATH,
                    HttpGet.METHOD_NAME,
                    null,
                    new TypeReference<List<Type.Browser>>() {}
            );
        }

        @Override
        public void delete(String id) {
            client.request(StrUtil.format("{}/{}", API_BROWSERS_PATH, id), HttpDelete.METHOD_NAME);
        }

        @Override
        public Type.HealthCheckResult healthCheck() {
            return client.request(
                    API_BROWSERS_PATH + "/health_check",
                    HttpPost.METHOD_NAME,
                    null,
                    new TypeReference<Type.HealthCheckResult>() {}
            );
        }
    }

    /**
     * 上下文管理实现
     */
    public static class ContextImpl implements Context {

        private final ClientImpl client;
        private final String browserId;

        public ContextImpl(ClientImpl client, String browserId) {
            this.client = client;
            this.browserId = browserId;
        }

        @Override
        public Type.Context create(Request.CreateContext req) {
            try {
                String path = StrUtil.format(API_CONTEXTS_PATH, browserId);
                Type.Context context = client.request(
                        path,
                        HttpPost.METHOD_NAME,
                        JSONUtil.toJsonStr(req),
                        new TypeReference<Type.Context>() {}
                );

                if (context == null) {
                    throw new RuntimeException("创建上下文失败: 响应为空");
                }
                return context;
            } catch (Exception e) {
                throw new RuntimeException("创建上下文失败: " + e.getMessage(), e);
            }
        }

        @Override
        public Type.Context get(String id) {
            String path = StrUtil.format(API_CONTEXTS_PATH + "/{}", browserId, id);
            return client.request(path, HttpGet.METHOD_NAME, null, new TypeReference<Type.Context>() {});
        }

        @Override
        public List<Type.Context> list() {
            String path = StrUtil.format(API_CONTEXTS_PATH, browserId);
            return client.request(path, HttpGet.METHOD_NAME, null, new TypeReference<List<Type.Context>>() {});
        }

        @Override
        public void delete(String id) {
            String path = StrUtil.format(API_CONTEXTS_PATH + "/{}", browserId, id);
            client.request(path, HttpDelete.METHOD_NAME);
        }

        @Override
        public void saveUserdata(String id) {
            String path = StrUtil.format(API_CONTEXTS_PATH + "/{}", browserId, id);
            client.request(path, HttpPut.METHOD_NAME);
        }

        @Override
        public Page page(String contextId) {
            return new PageImpl(client, browserId, contextId);
        }
    }

    /**
     * 页面管理实现
     */
    public static class PageImpl implements Page {

        private final ClientImpl client;
        private final String browserId;
        private final String contextId;

        public PageImpl(ClientImpl client, String browserId, String contextId) {
            this.client = client;
            this.browserId = browserId;
            this.contextId = contextId;
        }

        /**
         * 构建页面操作的基础路径
         *
         * @return 基础路径字符串
         */
        private String buildBasePath() {
            return StrUtil.format(API_PAGES_PATH, browserId, contextId);
        }

        /**
         * 构建带附加段的路径
         *
         * @param segments 要追加的路径段
         * @return 完整路径字符串
         */
        private String buildPath(String... segments) {
            String basePath = buildBasePath();
            if (segments.length == 0) {
                return basePath;
            }
            return basePath + "/" + String.join("/", segments);
        }

        @Override
        public Type.Context delete(String id) {
            return client.request(
                    buildPath(id),
                    HttpDelete.METHOD_NAME,
                    null,
                    new TypeReference<Type.Context>() {}
            );
        }

        @Override
        public Type.Context create(String url) {
            JSONObject body = new JSONObject();
            body.set("url", url);
            return client.request(
                    buildBasePath(),
                    HttpPost.METHOD_NAME,
                    body.toString(),
                    new TypeReference<Type.Context>() {}
            );
        }

        @Override
        public Request.JSResult execute(String expression) {
            JSONObject body = new JSONObject();
            body.set("expression", expression);
            return client.request(
                    buildPath("execute"),
                    HttpPost.METHOD_NAME,
                    body.toString(),
                    new TypeReference<Request.JSResult>() {}
            );
        }

        @Override
        public Map<String, Object> executeCdp(String method, Map<String, Object> params) {
            Map<String, Object> seleniumParams = new HashMap<>(2);
            seleniumParams.put("method", method);
            seleniumParams.put("params", params);
            String formatParams = SELENIUM_JSON.toJson(seleniumParams);

            Map<String, Object> result = client.request(
                    buildPath("execute_cdp"),
                    HttpPost.METHOD_NAME,
                    formatParams,
                    new TypeReference<Map<String, Object>>() {}
            );
            convertIntegerToLong(result);
            return result;
        }

        @Override
        public Type.Context gotoUrl(String url) {
            JSONObject body = new JSONObject();
            body.set("url", url);
            return client.request(
                    buildPath("goto"),
                    HttpPost.METHOD_NAME,
                    body.toString(),
                    new TypeReference<Type.Context>() {}
            );
        }

        @Override
        public void executeElement(Request.Action action) {
            client.request(
                    buildPath("element"),
                    HttpPost.METHOD_NAME,
                    JSONUtil.toJsonStr(action)
            );
        }

        @Override
        public void goBack() {
            client.request(buildPath("go_back"), HttpPost.METHOD_NAME);
        }

        @Override
        public void goForward() {
            client.request(buildPath("go_forward"), HttpPost.METHOD_NAME);
        }

        @Override
        public Request.JSResult findElement(String selector) {
            JSONObject body = new JSONObject();
            body.set("selector", selector);
            return client.request(
                    buildPath("find_element"),
                    HttpPost.METHOD_NAME,
                    body.toString(),
                    new TypeReference<Request.JSResult>() {}
            );
        }

        @Override
        public Type.Size getElementSize(String elementId) {
            return client.request(
                    buildPath("element", elementId, "get_size"),
                    HttpPost.METHOD_NAME,
                    null,
                    new TypeReference<Type.Size>() {}
            );
        }
    }

    private final String endpoint;
    private final Browser browser;

    /**
     * 构造函数，使用指定的端点创建客户端
     *
     * @param endpoint CDP服务端点URL
     */
    public ClientImpl(String endpoint) {
        this.endpoint = endpoint;
        this.browser = new BrowserImpl(this);
    }

    /**
     * 从相对路径构建完整URL
     *
     * @param url 相对URL路径
     * @return 完整URL
     */
    public String buildUrl(String url) {
        return this.endpoint + url;
    }

    @Override
    public Browser browser() {
        return browser;
    }

    @Override
    public Context context(String browserId) {
        return new ContextImpl(this, browserId);
    }

    @Override
    public void request(String url, String method) {
        HttpUtil.request(buildUrl(url), method, null);
    }

    public void request(String url, String method, String body) {
        HttpUtil.request(buildUrl(url), method, body);
    }

    @Override
    public <T> T request(String url, String method, String body, TypeReference<T> typeReference) {
        return HttpUtil.request(buildUrl(url), method, body, typeReference);
    }

    /**
     * 递归将 Map 中的 Integer 和 Short 转为 Long。
     * 用于与 Selenium 类型系统兼容。
     *
     * @param map 待处理的 Map
     */
    private static void convertIntegerToLong(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : new HashMap<>(map).entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                convertIntegerToLong((Map<String, Object>) value);
            } else if (value instanceof List) {
                convertListIntegerToLong((List<?>) value);
            } else if (value instanceof Integer) {
                map.put(entry.getKey(), ((Integer) value).longValue());
            } else if (value instanceof Short) {
                map.put(entry.getKey(), ((Short) value).longValue());
            }
        }
    }

    /**
     * 递归处理 List 中的 Map 元素，将其内 Integer/Short 转为 Long（与 convertIntegerToLong 配合）。
     *
     * @param list 待处理的 List（仅处理元素类型为 Map 的项）
     */
    private static void convertListIntegerToLong(List<?> list) {
        for (Object item : list) {
            if (item instanceof Map) {
                convertIntegerToLong((Map<String, Object>) item);
            }
        }
    }
}