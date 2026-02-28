package com.huawei.browsergateway.entity.event;

public enum EventTypeEnum {
    BROWSER_USER_DATA_DEAL_LOGIN("browser_user_datadeal_login", "云浏览器用户datadeal登录埋点", "server"),
    APP_FLOW_RATE_STAT("app_flow_rate_stat", "云浏览器应用流量统计事件", "server");

    private final String event;
    private final String eventDesc;
    private final String eventTrigger;

    EventTypeEnum(String event, String eventDesc, String eventTrigger) {
        this.event = event;
        this.eventDesc = eventDesc;
        this.eventTrigger = eventTrigger;
    }

    public String getEvent() {
        return event;
    }

    public String getEventDesc() {
        return eventDesc;
    }

    public String getEventTrigger() {
        return eventTrigger;
    }
}