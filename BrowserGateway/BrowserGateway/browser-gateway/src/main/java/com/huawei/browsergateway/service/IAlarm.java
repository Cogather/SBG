package com.huawei.browsergateway.service;

import com.huawei.browsergateway.entity.alarm.AlarmEvent;

/**
 * 告警管理接口，负责告警的发送与清除
 */
public interface IAlarm {

    /**
     * 发送告警事件
     *
     * @param alarmEvent 告警事件，包含告警码和消息
     */
    void sendAlarm(AlarmEvent alarmEvent);

    /**
     * 清除已发送的告警
     *
     * @param alarmEvent 需要清除的告警事件
     */
    void clearAlarm(AlarmEvent alarmEvent);
}
