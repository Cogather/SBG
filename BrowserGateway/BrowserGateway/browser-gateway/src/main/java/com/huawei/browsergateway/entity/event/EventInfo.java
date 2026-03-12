package com.huawei.browsergateway.entity.event;

import lombok.Data;

import java.util.Date;

/**
 * 事件信息
 */
@Data
public class EventInfo<T> {
    /** 服务名称 */
    private String service;
    /** 事件标识 */
    private String event;
    /** 事件描述 */
    private String eventDesc;
    /** 事件触发方 */
    private String eventTrigger;
    /** 事件时间 */
    private Date eventTime;
    /** 环境标识 */
    private String env;
    /** 主机名 */
    private String hostname;
    /** 事件对象 */
    private String object;
    /** 服务实例名称 */
    private String serviceInstanceName;
    /** 事件数据 */
    private T eventData;

    /**
     * 创建事件信息
     *
     * @param data      事件数据
     * @param eventType 事件类型枚举
     * @param eventTime 事件时间，为null时取当前时间
     * @param <T>       事件数据类型
     * @return EventInfo实例
     */
    public static <T> EventInfo<T> create(T data, EventTypeEnum eventType, Date eventTime) {
        if (eventTime == null) {
            eventTime = new Date();
        }
        EventInfo<T> eventInfo = new EventInfo<>();
        eventInfo.setEvent(eventType.getEvent());
        eventInfo.setEventDesc(eventType.getEventDesc());
        eventInfo.setEventTrigger(eventType.getEventTrigger());
        eventInfo.setEventData(data);
        eventInfo.setEventTime(eventTime);
        return eventInfo;
    }
}
