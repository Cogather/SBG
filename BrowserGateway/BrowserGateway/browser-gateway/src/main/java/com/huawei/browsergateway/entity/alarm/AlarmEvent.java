package com.huawei.browsergateway.entity.alarm;

import com.huawei.browsergateway.common.enums.ErrorCodeEnum;
import lombok.Data;

/**
 * 告警事件
 */
@Data
public class AlarmEvent {
    private ErrorCodeEnum alarmCodeEnum;
    private String eventMessage;
}
