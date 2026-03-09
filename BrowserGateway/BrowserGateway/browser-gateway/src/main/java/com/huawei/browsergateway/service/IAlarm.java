package com.huawei.browsergateway.service;

import com.huawei.browsergateway.entity.alarm.AlarmEvent;

/**
 * 告警管理接口
 */
public interface IAlarm {
    
    /**
     * 发送告警
     */
     void sendAlarm(AlarmEvent alarmEvent);
    
    /**
     * 清除告警
     */
    void clearAlarm(AlarmEvent alarmEvent);
}
