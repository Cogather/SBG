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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HttpUtil {
    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);
    @Getter
    private static final CloseableHttpClient httpClient;
    private static final PoolingHttpClientConnectionManager connectionManager;

    static {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100); // 池内最大总连接数
        connectionManager.setDefaultMaxPerRoute(20);    // 每个域名/路由最大连接数
        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)    // 绑定连接池
                .evictIdleConnections(TimeValue.ofMinutes(1))   // 自动清理空闲连接
                .build();
    }


    public static void request(String url, String method, String body) {
        HttpUriRequest httpUriRequest = ClassicHttpRequests.create(method, url);
        if (!StrUtil.isEmpty(body)) {
            StringEntity stringEntity = new StringEntity(body);
            httpUriRequest.setEntity(stringEntity);
        }
        try {
            httpClient.execute(httpUriRequest, response -> {
                if (response.getCode() != 200) {
                    log.warn("requeset for {}, get code {}", url, response.getCode());
                }
                return null;
            });
        } catch (IOException e) {
            log.error("request for {}, get error", url, e);
            throw new RuntimeException(e);
        }
    }

    public static  <T> T request(String url, String method, String body, TypeReference<T> typeReference) {
        HttpUriRequest httpUriRequest = ClassicHttpRequests.create(method, url);
        if (!StrUtil.isEmpty(body)) {
            StringEntity stringEntity = new StringEntity(body);
            httpUriRequest.setEntity(stringEntity);
        }
        T result;
        try {
            result = httpClient.execute(httpUriRequest, response -> {
                if (response.getCode() != 200) {
                    log.warn("requeset for {}, get code {}", url, response.getCode());
                    return null;
                }
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return null;
                }
                return JSONUtil.toBean(EntityUtils.toString(entity), typeReference, true);
            });
        } catch (IOException e) {
            log.error("request for {}, get error", url, e);
            throw new RuntimeException(e);
        }
        return result;
    }

}