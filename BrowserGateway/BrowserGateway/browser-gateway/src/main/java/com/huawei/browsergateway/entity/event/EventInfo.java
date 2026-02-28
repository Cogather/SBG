package com.huawei.browsergateway.entity.event;

import lombok.Data;

import java.util.Date;

@Data
public class EventInfo<T> {
    private String service;
    private String event;
    private String eventDesc;
    private String eventTrigger;
    private Date eventTime;
    private String env;
    private String hostname;
    private String object;
    private String serviceInstanceName;
    private T eventData;

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