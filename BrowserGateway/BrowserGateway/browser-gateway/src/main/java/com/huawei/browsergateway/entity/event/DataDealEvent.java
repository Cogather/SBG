package com.huawei.browsergateway.entity.event;

import com.huawei.browsergateway.util.encode.Message;
import lombok.Data;

import java.util.Date;

/**
 * 数据处理事件
 */
@Data
public class DataDealEvent extends BaseEvent {
    /** 登录时间 */
    private Date loginTime;

    /**
     * 从消息对象构造数据处理事件
     *
     * @param message 消息对象
     */
    public DataDealEvent(Message message) {
        super(message);
    }
}
