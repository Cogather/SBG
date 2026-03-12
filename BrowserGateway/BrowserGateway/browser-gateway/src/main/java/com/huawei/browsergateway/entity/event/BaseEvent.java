package com.huawei.browsergateway.entity.event;

import com.huawei.browsergateway.util.encode.Message;
import lombok.Data;

/**
 * 基础事件
 */
@Data
public class BaseEvent {
    /** IMEI号 */
    private String imei;
    /** IMSI号 */
    private String imsi;
    /** 扩展类型 */
    private String exttype;
    /** 厂商 */
    private String hsman;
    /** 机型 */
    private String hstype;

    /** 默认构造方法 */
    public BaseEvent() {
    }

    /**
     * 从消息对象构造基础事件
     *
     * @param message 消息对象
     */
    public BaseEvent(Message message) {
        this.imei = message.getImei();
        this.imsi = message.getImsi();
        this.exttype = message.getExtType();
        this.hsman = message.getFactory();
        this.hstype = message.getDevType();
    }
}
