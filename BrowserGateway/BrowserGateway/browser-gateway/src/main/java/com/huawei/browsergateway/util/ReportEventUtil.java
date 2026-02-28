package com.huawei.browsergateway.util;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.entity.event.EventInfo;
import com.moon.cloud.browser.sdk.model.pojo.ReportEvent;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ReportEventUtil {
    private static final Logger log = LoggerFactory.getLogger(ReportEventUtil.class);
    private static final String EVENT_API = "/server/event/v1/uploadEvent";
    public static void reportSdkEvent(ReportEvent<Object> event, String endpoint) {
        reportEvent(event,endpoint);
    }
    public static <T> void reportServerEvent(EventInfo<T> event, String endpoint) {
        reportEvent(event,endpoint);
    }

    private static void reportEvent(Object event, String endpoint) {
        String url = String.format("http://%s%s", endpoint, EVENT_API);

        //format Date
        JSONConfig jsonConfig = JSONConfig.create()
                .setDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            HttpUtil.request(url, HttpPost.METHOD_NAME, JSONUtil.toJsonStr(event, jsonConfig));
        } catch (Exception e) {
            log.error("report event failed, event: {}", JSONUtil.toJsonStr(event, jsonConfig), e);
        }
    }}
