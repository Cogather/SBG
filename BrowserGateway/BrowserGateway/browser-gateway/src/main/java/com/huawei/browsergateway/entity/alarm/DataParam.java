package com.huawei.browsergateway.entity.alarm;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
public class DataParam {

    private String appId;

    private String alarmIds;

    private String alarmGenTimeUTC;

    private String syncNO;

    private String appName;

    private String alarmName;

    private String clearUserIP;

    private String eventType;

    private String serialNO;

    private String repeatTimes;

    private String alarmId;

    private String clearUser;

    private String alarmClearType;

    private String alarmLevel;

    private String location;

    private String alarmClearTimeUTC;

    private String appendInfo;

}