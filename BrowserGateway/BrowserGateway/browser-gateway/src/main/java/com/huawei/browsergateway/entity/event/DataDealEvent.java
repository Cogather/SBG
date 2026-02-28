package com.huawei.browsergateway.entity.event;

import com.huawei.browsergateway.util.encode.Message;
import lombok.Data;

import java.util.Date;

@Data
public class DataDealEvent extends BaseEvent{
    private Date loginTime;
    public DataDealEvent(Message message) {
        super(message);
    }
}