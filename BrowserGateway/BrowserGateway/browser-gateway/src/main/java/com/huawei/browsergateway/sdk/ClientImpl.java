package com.huawei.browsergateway.sdk;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.util.HttpUtil;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.openqa.selenium.json.Json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP客户端实现类
 * 实现DriverClient接口，提供与CDP服务通信的HTTP客户端功能
 */
public class ClientImpl implements DriverClient {

    private static final Json SELENIUM_JSON = new Json();

    /** CDP服务端点地址 */
    private final String endpoint;

    /** 浏览器管理接口实例 */
    private final DriverClient.Browser browser;

    /**
     * 构造函数
     *
     * @param endpoint CDP服务端点地址
     */
    public ClientImpl(String endpoint) {
        this.endpoint = endpoint;
        this.browser = new BrowserImpl(this);
    }

    /**
     * 构建完整URL
     *
     * @param url 相对路径
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

    /**
     * 发送HTTP请求（无返回类型）
     *
     * @param url    请求URL
     * @param method 请求方法
     * @param body   请求体
     */
    public void request(String url, String method, String body) {
        HttpUtil.request(buildUrl(url), method, body);
    }

    @Override
    public <T> T request(String url, String method, String body, TypeReference<T> typeReference) {
        return HttpUtil.request(buildUrl(url), method, body, typeReference);
    }

    /**
     * 将Map中的Integer和Short转换为Long
     * 用于CDP命令执行结果的类型转换
     *
     * @param map 待转换的Map
     */
    private static void convertIntegerToLong(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : new HashMap<>(map).entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                // 如果值是Map，递归处理
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                convertIntegerToLong(nestedMap);
            } else if (value instanceof List) {
                // 如果值是List，遍历处理每个元素
                List<?> list = (List<?>) value;
                for (Object item : list) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        convertIntegerToLong(itemMap);
                    }
                }
            } else if (value instanceof Integer) {
                // 将Integer转换为Long
                map.put(entry.getKey(), ((Integer) value).longValue());
            } else if (value instanceof Short) {
                // 将Short转换为Long
                map.put(entry.getKey(), ((Short) value).longValue());
            }
        }
    }

    /**
     * 浏览器管理接口实现类
     */
    public static class BrowserImpl implements DriverClient.Browser {

        private final DriverClient client;

        /**
         * 构造函数
         *
         * @param client 驱动客户端实例
         */
        public BrowserImpl(DriverClient client) {
            this.client = client;
        }

        @Override
        public Type.Browser create(Request.CreateBrowser request) {
            try {
                String url = StrUtil.format("/api/browsers");
                String requestBody = JSONUtil.toJsonStr(request);
                Type.Browser browser = client.request(url, HttpPost.METHOD_NAME, requestBody,
                        new TypeReference<>() {
                        });

                if (browser == null) {
                    throw new RuntimeException("failed to create browsers");
                }

                return browser;
            } catch (Exception e) {
                throw new RuntimeException("failed to create browsers: " + e.getMessage(), e);
            }
        }

        @Override
        public Type.Browser get(String id) {
            String url = StrUtil.format("/api/browsers/{}", id);
            return client.request(url, HttpGet.METHOD_NAME, null, new TypeReference<>() {
            });
        }

        @Override
        public List<Type.Browser> list() {
            String url = StrUtil.format("/api/browsers");
            return client.request(url, HttpGet.METHOD_NAME, null, new TypeReference<>() {
            });
        }

        @Override
        public void delete(String id) {
            String url = StrUtil.format("/api/browsers/{}", id);
            client.request(url, HttpDelete.METHOD_NAME);
        }

        @Override
        public Type.HealthCheckResult healthCheck() {
            String url = StrUtil.format("/api/browsers/health_check");
            return client.request(url, HttpPost.METHOD_NAME, null, new TypeReference<>() {
            });
        }
    }

    /**
     * 上下文管理接口实现类
     */
    public static class ContextImpl implements DriverClient.Context {

        private final ClientImpl client;
        private final String browserId;

        /**
         * 构造函数
         *
         * @param client    客户端实例
         * @param browserId 浏览器ID
         */
        public ContextImpl(ClientImpl client, String browserId) {
            this.client = client;
            this.browserId = browserId;
        }

        @Override
        public Type.Context create(Request.CreateContext request) {
            try {
                String url = StrUtil.format("/api/browsers/{}/contexts", browserId);
                String requestBody = JSONUtil.toJsonStr(request);
                Type.Context context = client.request(url, HttpPost.METHOD_NAME, requestBody,
                        new TypeReference<>() {
                        });

                if (context == null) {
                    throw new RuntimeException("failed to create user interface");
                }
                return context;
            } catch (Exception e) {
                throw new RuntimeException("failed to create user interface: " + e.getMessage(), e);
            }
        }

        @Override
        public Type.Context get(String id) {
            String url = StrUtil.format("/api/browsers/{}/contexts/{}", browserId, id);
            return client.request(url, HttpGet.METHOD_NAME, null, new TypeReference<>() {
            });
        }

        @Override
        public List<Type.Context> list() {
            String url = StrUtil.format("/api/browsers/{}/contexts", browserId);
            return client.request(url, HttpGet.METHOD_NAME, null, new TypeReference<>() {
            });
        }

        @Override
        public void delete(String id) {
            String url = StrUtil.format("/api/browsers/{}/contexts/{}", browserId, id);
            client.request(url, HttpDelete.METHOD_NAME);
        }

        @Override
        public void saveUserdata(String contextId) {
            String url = StrUtil.format("/api/browsers/{}/contexts/{}", browserId, contextId);
            client.request(url, HttpPut.METHOD_NAME);
        }

        @Override
        public Page page(String contextId) {
            return new PageImpl(client, browserId, contextId);
        }
    }

    /**
     * 页面管理接口实现类
     */
    public static class PageImpl implements Page {

        private final ClientImpl client;
        private final String browserId;
        private final String contextId;

        /**
         * 构造函数
         *
         * @param client    客户端实例
         * @param browserId 浏览器ID
         * @param contextId 上下文ID
         */
        public PageImpl(ClientImpl client, String browserId, String contextId) {
            this.client = client;
            this.browserId = browserId;
            this.contextId = contextId;
        }

        @Override
        public Type.Context delete(String id) {
            String url = StrUtil.format("/api/browsers/{}/contexts/{}/pages/{}", browserId, contextId, id);
            return client.request(url, HttpDelete.METHOD_NAME, null, new TypeReference<>() {
            });
        }

        @Override
        public Type.Context create(String url) {
            JSONObject body = new JSONObject();
            body.set("url", url);

            String requestUrl = StrUtil.format("/api/browsers/{}/contexts/{}/pages", browserId, contextId);
            return client.request(requestUrl, HttpPost.METHOD_NAME, body.toString(), new TypeReference<>() {
            });
        }

        @Override
        public Request.JSResult execute(String expression) {
            JSONObject body = new JSONObject();
            body.set("expression", expression);

            String url = StrUtil.format("/api/browsers/{}/contexts/{}/pages/execute", browserId, contextId);
            return client.request(url, HttpPost.METHOD_NAME, body.toString(), new TypeReference<>() {
            });
        }

        @Override
        public Map<String, Object> executeCdp(String method, Map<String, Object> params) {
            Map<String, Object> seleniumParams = new HashMap<>(2);
            seleniumParams.put("method", method);
            seleniumParams.put("params", params);
            String formatParams = SELENIUM_JSON.toJson(seleniumParams);

            String url = StrUtil.format("/api/browsers/{}/contexts/{}/pages/execute_cdp", browserId, contextId);
            Map<String, Object> result = client.request(url, HttpPost.METHOD_NAME, formatParams,
                    new TypeReference<>() {
                    });
            convertIntegerToLong(result);
            return result;
        }

        @Override
        public Type.Context gotoUrl(String url) {
            JSONObject body = new JSONObject();
            body.set("url", url);

            String requestUrl = StrUtil.format("/api/browsers/{}/contexts/{}/pages/goto", browserId, contextId);
            return client.request(requestUrl, HttpPost.METHOD_NAME, body.toString(), new TypeReference<>() {
            });
        }

        @Override
        public void executeElement(Request.Action action) {
            String url = StrUtil.format("/api/browsers/{}/contexts/{}/pages/element", browserId, contextId);
            String requestBody = JSONUtil.toJsonStr(action);
            client.request(url, HttpPost.METHOD_NAME, requestBody);
        }

        @Override
        public void goBack() {
            String url = StrUtil.format("/api/browsers/{}/contexts/{}/pages/go_back", browserId, contextId);
            client.request(url, HttpPost.METHOD_NAME);
        }

        @Override
        public void goForward() {
            String url = StrUtil.format("/api/browsers/{}/contexts/{}/pages/go_forward", browserId, contextId);
            client.request(url, HttpPost.METHOD_NAME);
        }

        @Override
        public Request.JSResult findElement(String selector) {
            JSONObject body = new JSONObject();
            body.set("selector", selector);

            String url = StrUtil.format("/api/browsers/{}/contexts/{}/pages/find_element", browserId, contextId);
            return client.request(url, HttpPost.METHOD_NAME, body.toString(), new TypeReference<>() {
            });
        }

        @Override
        public Type.Size getElementSize(String elementId) {
            String url = StrUtil.format("/api/browsers/{}/contexts/{}/pages/element/{}/get_size",
                    browserId, contextId, elementId);
            return client.request(url, HttpPost.METHOD_NAME, null, new TypeReference<>() {
            });
        }
    }
}
