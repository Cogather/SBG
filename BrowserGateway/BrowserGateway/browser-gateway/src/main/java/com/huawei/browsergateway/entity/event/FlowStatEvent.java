package com.huawei.browsergateway.entity.event;

import com.huawei.browsergateway.util.encode.Message;
import lombok.Data;

import java.util.Date;

/**
 * 流量统计事件
 */
@Data
public class FlowStatEvent extends BaseEvent {
    /** 扩展机型 */
    private String extendModel;
    /** 应用类型 */
    private int appType;
    /** 应用ID */
    private int appId;
    /** 屏幕高度 */
    private int scheight;
    /** 屏幕宽度 */
    private int scwidth;
    /** 服务类型 */
    private String serviceType;
    /** 网络类型 */
    private int networkType;
    /** IMEI1 */
    private String imei1;
    /** IMEI2 */
    private String imei2;
    /** 数据量（字节） */
    private long dataSize;
    /** 退出时间 */
    private Date exitTime;

    /**
     * 从消息对象构造流量统计事件
     *
     * @param message 消息对象
     */
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
