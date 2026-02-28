package com.huawei.browsergateway.entity.alarm;

import com.huawei.browsergateway.entity.enums.AlarmEnum;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class AlarmEvent {

    private AlarmEnum alarmCodeEnum;

    private String eventMessage;

}