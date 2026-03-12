package com.huawei.browsergateway.entity.alarm;

import com.huawei.browsergateway.entity.enums.AlarmEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 告警事件
 */
@Data
@AllArgsConstructor
public class AlarmEvent {
    /** 告警枚举 */
    private AlarmEnum alarmCodeEnum;

    /** 事件消息 */
    private String eventMessage;
}
