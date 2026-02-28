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
 * HTTP客户端实现，与CDP服务通信
 */
public class ClientImpl implements DriverClient {
    private static final Json SELENIUM_JSON = new Json();

    public static class BrowserImpl implements DriverClient.Browser {
        private final DriverClient client;

        public BrowserImpl(DriverClient client) {
            this.client = client;
        }

        @Override
        public Type.Browser create(Request.CreateBrowser req) {
            try {
                Type.Browser request = client.request(StrUtil.format("/api/browsers"), HttpPost.METHOD_NAME, JSONUtil.toJsonStr(req), new TypeReference<>() {
                });

                if (request == null) {
                    throw new RuntimeException("failed to create browsers");
                }

                return request;
            } catch (Exception e) {
                throw new RuntimeException("failed to create browsers" + e.getMessage());
            }
        }

        @Override
        public Type.Browser get(String id) {
            return client.request(StrUtil.format("/api/browsers/{}", id), HttpGet.METHOD_NAME, null, new TypeReference<>() {
            });
        }

        @Override
        public List<Type.Browser> list() {
            return client.request(StrUtil.format("/api/browsers"), HttpGet.METHOD_NAME, null, new TypeReference<>() {
            });
        }

        @Override
        public void delete(String id) {
            client.request(StrUtil.format("/api/browsers/{}", id), HttpDelete.METHOD_NAME);
        }

        @Override
        public Type.HealthCheckResult healthCheck() {
            return client.request(StrUtil.format("/api/browsers/health_check"), HttpPost.METHOD_NAME, null
                    , new TypeReference<>() {
                    });
        }
    }

    public static class ContextImpl implements DriverClient.Context {

        private final ClientImpl client;
        private final String browserId;

        public ContextImpl(ClientImpl client, String browserId) {
            this.client = client;
            this.browserId = browserId;
        }

        @Override
        public Type.Context create(Request.CreateContext req) {
            try {
                Type.Context request = client.request(StrUtil.format("/api/browsers/{}/contexts", browserId), HttpPost.METHOD_NAME, JSONUtil.toJsonStr(req), new TypeReference<>() {
                });

                if (request == null) {
                    throw new RuntimeException("failed to create user interface");
                }
                return request;
            } catch (Exception e) {
                throw new RuntimeException("failed to create user interface" + e.getMessage());
            }
        }

        @Override
        public Type.Context get(String id) {
            return client.request(StrUtil.format("/api/browsers/{}/contexts/{}", browserId, id), HttpGet.METHOD_NAME, null, new TypeReference<>() {
            });
        }

        @Override
        public List<Type.Context> list() {
            return client.request(StrUtil.format("/api/browsers/{}/contexts", browserId), HttpGet.METHOD_NAME, null, new TypeReference<>() {
            });
        }

        @Override
        public void delete(String id) {
            client.request(StrUtil.format("/api/browsers/{}/contexts/{}", browserId, id), HttpDelete.METHOD_NAME);
        }

        @Override
        public void saveUserdata(String id) {
            client.request(StrUtil.format("/api/browsers/{}/contexts/{}", browserId, id), HttpPut.METHOD_NAME);
        }

        @Override
        public Page page(String contextId) {
            return new PageImpl(client, browserId, contextId);
        }
    }

    public static class PageImpl implements Page {

        private final ClientImpl client;
        private final String browserId;
        private final String contextId;

        public PageImpl(ClientImpl client, String browserId, String contextId) {
            this.client = client;
            this.browserId = browserId;
            this.contextId = contextId;
        }

        @Override
        public Type.Context delete(String id) {
            return client.request(StrUtil.format("/api/browsers/{}/contexts/{}/pages/{}", browserId, contextId, id), HttpDelete.METHOD_NAME, null, new TypeReference<>() {
            });
        }

        @Override
        public Type.Context create(String url) {
            JSONObject body = new JSONObject();
            body.set("url", url);

            return client.request(StrUtil.format("/api/browsers/{}/contexts/{}/pages", browserId, contextId), HttpPost.METHOD_NAME, body.toString(), new TypeReference<>() {
            });
        }

        @Override
        public Request.JSResult execute(String expression) {
            JSONObject body = new JSONObject();
            body.set("expression", expression);

            return client.request(StrUtil.format("/api/browsers/{}/contexts/{}/pages/execute", browserId, contextId), HttpPost.METHOD_NAME, body.toString(), new TypeReference<>() {
            });
        }

        @Override
        public Map<String, Object> executeCdp(String method, Map<String, Object> params) {
            Map<String, Object> seleniumParams = new HashMap<>(2);
            seleniumParams.put("method", method);
            seleniumParams.put("params", params);
            String formatParams = SELENIUM_JSON.toJson(seleniumParams);

            Map<String, Object> result = client.request(StrUtil.format("/api/browsers/{}/contexts/{}/pages/execute_cdp", browserId, contextId), HttpPost.METHOD_NAME, formatParams, new TypeReference<>() {
            });
            convertIntegerToLong(result);
            return result;
        }

        @Override
        public Type.Context gotoUrl(String url) {
            JSONObject body = new JSONObject();
            body.set("url", url);

            return client.request(StrUtil.format("/api/browsers/{}/contexts/{}/pages/goto", browserId
                    , contextId), HttpPost.METHOD_NAME, body.toString(), new TypeReference<>() {
            });
        }

        @Override
        public void executeElement(Request.Action action) {
            client.request(StrUtil.format("/api/browsers/{}/contexts/{}/pages/element", browserId, contextId)
                    , HttpPost.METHOD_NAME, JSONUtil.toJsonStr(action));
        }

        @Override
        public void goBack() {
            client.request(StrUtil.format("/api/browsers/{}/contexts/{}/pages/go_back", browserId, contextId)
                    , HttpPost.METHOD_NAME);
        }

        @Override
        public void goForward() {
            client.request(StrUtil.format("/api/browsers/{}/contexts/{}/pages/go_forward", browserId, contextId)
                    , HttpPost.METHOD_NAME);
        }

        @Override
        public Request.JSResult findElement(String selector) {
            JSONObject body = new JSONObject();
            body.set("selector", selector);
            return client.request(StrUtil.format("/api/browsers/{}/contexts/{}/pages/find_element", browserId
                    , contextId), HttpPost.METHOD_NAME, body.toString(), new TypeReference<>() {
            });
        }

        @Override
        public Type.Size getElementSize(String elementId) {
            return client.request(StrUtil.format("/api/browsers/{}/contexts/{}/pages/element/{}/get_size"
                    , browserId, contextId, elementId), HttpPost.METHOD_NAME, null, new TypeReference<>() {
            });
        }


    }

    private final String endpoint;
    private final DriverClient.Browser browser;

    public ClientImpl(String endpoint) {
        this.endpoint = endpoint;
        browser = new BrowserImpl(this);
    }

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


    public void request(String url, String method) {
        HttpUtil.request(buildUrl(url), method, null);
    }

    public void request(String url, String method, String body) {
        HttpUtil.request(buildUrl(url), method, body);
    }

    public <T> T request(String url, String method, String body, TypeReference<T> typeReference) {
        return HttpUtil.request(buildUrl(url), method, body, typeReference);
    }

    private static void convertIntegerToLong(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : new HashMap<>(map).entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                // 如果值是 Map，递归处理
                convertIntegerToLong((Map<String, Object>) value);
            } else if (value instanceof List) {
                // 如果值是 List，遍历处理每个元素
                List<?> list = (List<?>) value;
                for (Object item : list) {
                    if (item instanceof Map) {
                        convertIntegerToLong((Map<String, Object>) item);
                    }
                }
            } else if (value instanceof Integer) {
                // 将 Integer 转换为 Long
                map.put(entry.getKey(), ((Integer) value).longValue());
            } else if (value instanceof Short) {
                // 将 Short 转换为 Long
                map.put(entry.getKey(), ((Short) value).longValue());
            }
        }
    }
}