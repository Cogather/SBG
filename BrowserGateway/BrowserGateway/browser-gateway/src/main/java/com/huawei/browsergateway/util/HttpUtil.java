package com.huawei.browsergateway.util;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.Getter;
import org.apache.hc.client5.http.classic.methods.ClassicHttpRequests;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.TimeValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/** HTTP 请求工具类，内部使用连接池复用连接 */
public final class HttpUtil {

    private static final Logger log = LogManager.getLogger(HttpUtil.class);

    @Getter
    private static final CloseableHttpClient httpClient;

    static {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);          // 连接池最大总连接数
        cm.setDefaultMaxPerRoute(20); // 每个路由最大连接数
        httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .evictIdleConnections(TimeValue.ofMinutes(1)) // 自动清理空闲连接
                .build();
    }

    private HttpUtil() {}

    /**
     * 发送 HTTP 请求，忽略响应体
     *
     * @param url    请求地址
     * @param method HTTP 方法（GET/POST 等）
     * @param body   请求体，为空时不设置
     */
    public static void request(String url, String method, String body) {
        // TODO: 实现HTTP请求逻辑，忽略响应体
    }

    /**
     * 发送 HTTP 请求并将响应体反序列化为指定类型
     *
     * @param url           请求地址
     * @param method        HTTP 方法
     * @param body          请求体，为空时不设置
     * @param typeReference 响应体目标类型
     * @param <T>           目标类型
     * @return 反序列化后的响应对象，非 200 或响应体为空时返回 null
     */
    public static <T> T request(String url, String method, String body, TypeReference<T> typeReference) {
        // TODO: 实现HTTP请求逻辑，将响应体反序列化为指定类型
        return null;
    }

    /** 构建请求并执行，统一处理 IO 异常 */
    private static <T> T execute(String url, String method, String body,
            org.apache.hc.core5.http.io.HttpClientResponseHandler<T> handler) {
        HttpUriRequest req = ClassicHttpRequests.create(method, url);
        if (!StrUtil.isEmpty(body)) {
            req.setEntity(new StringEntity(body));
        }
        try {
            return httpClient.execute(req, handler);
        } catch (IOException e) {
            log.error("request for {} failed", url, e);
            throw new RuntimeException(e);
        }
    }
}
