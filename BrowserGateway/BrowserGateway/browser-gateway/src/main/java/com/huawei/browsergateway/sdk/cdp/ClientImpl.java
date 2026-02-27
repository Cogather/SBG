package com.huawei.browsergateway.sdk.cdp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * HTTP客户端实现，与CDP服务通信
 */
public class ClientImpl implements DriverClient {
    
    private static final Logger log = LoggerFactory.getLogger(ClientImpl.class);
    
    private final String endpoint;
    private final CloseableHttpClient httpClient;
    
    /**
     * 构造函数
     * 
     * @param endpoint CDP服务端点URL
     */
    public ClientImpl(String endpoint) {
        this.endpoint = endpoint != null ? endpoint.trim() : "http://127.0.0.1:8000";
        this.httpClient = HttpClients.createDefault();
        log.info("创建ClientImpl，端点: {}", this.endpoint);
    }
    
    /**
     * 执行HTTP请求
     * 
     * @param url 请求URL
     * @param method HTTP方法（GET, POST, DELETE等）
     * @param body 请求体（JSON字符串）
     * @param typeReference 响应类型引用
     * @return 响应对象
     */
    public <T> T request(String url, String method, String body, TypeReference<T> typeReference) {
        try {
            String fullUrl = endpoint + url;
            log.debug("执行HTTP请求: {} {}, body={}", method, fullUrl, body);
            
            CloseableHttpResponse response = null;
            try {
                if ("GET".equalsIgnoreCase(method)) {
                    HttpGet request = new HttpGet(fullUrl);
                    response = httpClient.execute(request);
                } else if ("POST".equalsIgnoreCase(method)) {
                    HttpPost request = new HttpPost(fullUrl);
                    if (body != null) {
                        StringEntity entity = new StringEntity(body, ContentType.APPLICATION_JSON);
                        request.setEntity(entity);
                    }
                    response = httpClient.execute(request);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    HttpDelete request = new HttpDelete(fullUrl);
                    response = httpClient.execute(request);
                } else {
                    throw new IllegalArgumentException("不支持的HTTP方法: " + method);
                }
                
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                
                if (statusCode >= 200 && statusCode < 300) {
                    if (typeReference != null && responseBody != null && !responseBody.trim().isEmpty()) {
                        T result = JSON.parseObject(responseBody, typeReference);
                        log.debug("HTTP请求成功: {} {}, statusCode={}", method, fullUrl, statusCode);
                        return result;
                    }
                    return null;
                } else {
                    log.warn("HTTP请求失败: {} {}, statusCode={}, response={}", method, fullUrl, statusCode, responseBody);
                    throw new RuntimeException("HTTP请求失败，状态码: " + statusCode);
                }
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        } catch (Exception e) {
            log.error("执行HTTP请求异常: {} {}", method, url, e);
            throw new RuntimeException("HTTP请求异常", e);
        }
    }
    
    @Override
    public Browser browser() {
        return new BrowserImpl();
    }
    
    @Override
    public Context context(String browserId) {
        return new ContextImpl(browserId);
    }
    
    /**
     * 浏览器管理实现
     */
    private class BrowserImpl implements Browser {
        @Override
        public Type.Browser create(Request.CreateBrowser req) {
            String body = JSON.toJSONString(req);
            return request(URL.BROWSER, "POST", body, new TypeReference<Type.Browser>() {});
        }
        
        @Override
        public Type.Browser get(String id) {
            return request(URL.BROWSER + "/" + id, "GET", null, new TypeReference<Type.Browser>() {});
        }
        
        @Override
        public List<Type.Browser> list() {
            return request(URL.BROWSER_LIST, "GET", null, new TypeReference<List<Type.Browser>>() {});
        }
        
        @Override
        public void delete(String id) {
            request(URL.BROWSER + "/" + id, "DELETE", null, null);
        }
        
        @Override
        public Type.HealthCheckResult healthCheck() {
            return request(URL.BROWSER_HEALTH, "GET", null, new TypeReference<Type.HealthCheckResult>() {});
        }
    }
    
    /**
     * 上下文管理实现
     */
    private class ContextImpl implements Context {
        private final String browserId;
        
        public ContextImpl(String browserId) {
            this.browserId = browserId;
        }
        
        @Override
        public Type.Context create(Request.CreateContext req) {
            String body = JSON.toJSONString(req);
            String url = URL.CONTEXT + (browserId != null ? "?browserId=" + browserId : "");
            return request(url, "POST", body, new TypeReference<Type.Context>() {});
        }
        
        @Override
        public Type.Context get(String id) {
            return request(URL.CONTEXT + "/" + id, "GET", null, new TypeReference<Type.Context>() {});
        }
        
        @Override
        public void delete(String id) {
            request(URL.CONTEXT + "/" + id, "DELETE", null, null);
        }
        
        @Override
        public void saveUserdata(String contextId) {
            request(URL.CONTEXT + "/" + contextId + "/save", "POST", null, null);
        }
        
        @Override
        public Page page(String contextId) {
            return new PageImpl(contextId);
        }
    }
    
    /**
     * 页面管理实现
     */
    private class PageImpl implements Page {
        private final String contextId;
        
        public PageImpl(String contextId) {
            this.contextId = contextId;
        }
        
        @Override
        public Type.Page create(String url) {
            String body = "{\"url\":\"" + url + "\"}";
            return request(URL.PAGE + "?contextId=" + contextId, "POST", body, new TypeReference<Type.Page>() {});
        }
        
        @Override
        public Request.JSResult execute(String expression) {
            String body = "{\"expression\":\"" + expression.replace("\"", "\\\"") + "\"}";
            return request(URL.PAGE_EXECUTE + "?contextId=" + contextId, "POST", body, new TypeReference<Request.JSResult>() {});
        }
        
        @Override
        public Map<String, Object> executeCdp(String method, Map<String, Object> params) {
            String body = JSON.toJSONString(Map.of("method", method, "params", params != null ? params : Map.of()));
            return request(URL.PAGE_CDP + "?contextId=" + contextId, "POST", body, new TypeReference<Map<String, Object>>() {});
        }
        
        @Override
        public Type.Page gotoUrl(String url) {
            String body = "{\"url\":\"" + url + "\"}";
            return request(URL.PAGE_GOTO + "?contextId=" + contextId, "POST", body, new TypeReference<Type.Page>() {});
        }
        
        @Override
        public String findElement(String selector) {
            String body = "{\"selector\":\"" + selector + "\"}";
            Map<String, Object> result = request(URL.PAGE + "/find?contextId=" + contextId, "POST", body, new TypeReference<Map<String, Object>>() {});
            return result != null ? (String) result.get("elementId") : null;
        }
    }
    
    /**
     * 关闭HTTP客户端
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (Exception e) {
            log.error("关闭HTTP客户端异常", e);
        }
    }
}
