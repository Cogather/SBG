package com.huawei.browsergateway.entity.event;

import com.huawei.browsergateway.util.encode.Message;
import lombok.Data;

import java.util.Date;

@Data
public class FlowStatEvent extends BaseEvent{
    private String extendModel;
    private int appType;
    private int appId;
    private int scheight;
    private int scwidth;
    private String serviceType;
    private int networkType;
    private String imei1;
    private String imei2;
    private long dataSize;
    private Date exitTime;

    public FlowStatEvent(Message message) {
        super(message);
        this.extendModel = message.getFactory();
        this.appType = message.getAppType();
        this.appId = message.getAppID();
        this.scheight = message.getLcdHeight();
        this.scwidth = message.getLcdWidth();
        this.networkType = message.getNetworkType();
        this.imei1 = message.getImei1();
        this.imei2 = message.getImei2();
    }
}