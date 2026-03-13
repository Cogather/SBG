package com.huawei.browsergateway.util;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.entity.event.EventInfo;
import com.moon.cloud.browser.sdk.model.pojo.ReportEvent;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 事件上报工具类 */
public final class ReportEventUtil {

    private static final Logger log = LogManager.getLogger(ReportEventUtil.class);
    private static final String EVENT_API = "/server/event/v1/uploadEvent";
    private static final JSONConfig DATE_CONFIG =
            JSONConfig.create().setDateFormat("yyyy-MM-dd HH:mm:ss");

    private ReportEventUtil() {}

    /**
     * 上报 SDK 事件
     *
     * @param event    SDK 事件对象
     * @param endpoint 目标服务地址（host:port）
     */
    public static void reportSdkEvent(ReportEvent<Object> event, String endpoint) {
        reportEvent(event, endpoint);
    }

    /**
     * 上报服务端事件
     *
     * @param event    服务端事件对象
     * @param endpoint 目标服务地址（host:port）
     */
    public static <T> void reportServerEvent(EventInfo<T> event, String endpoint) {
        reportEvent(event, endpoint);
    }

    /** 将事件序列化后 POST 到事件接口 */
    private static void reportEvent(Object event, String endpoint) {
        String url = String.format("http://%s%s", endpoint, EVENT_API);
        String body = JSONUtil.toJsonStr(event, DATE_CONFIG);
        try {
            HttpUtil.request(url, HttpPost.METHOD_NAME, body);
        } catch (Exception e) {
            log.error("report event failed, event: {}", body, e);
        }
    }
}
