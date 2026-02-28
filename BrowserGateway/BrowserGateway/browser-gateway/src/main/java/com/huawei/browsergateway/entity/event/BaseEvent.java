package com.huawei.browsergateway.entity.event;

import com.huawei.browsergateway.util.encode.Message;

public class BaseEvent {
    private String imei;
    private String imsi;
    private String exttype;
    private String hsman;
    private String hstype;
    public BaseEvent() {
    }
    
    public BaseEvent(Message message) {
        this.imei = message.getImei();
        this.imsi = message.getImsi();
        this.exttype = message.getExtType();
        this.hsman = message.getFactory();
        this.hstype = message.getDevType();
    }
}
