package com.huawei.browsergateway.entity.alarm;

import lombok.Data;

/**
 * 告警数据参数
 */
@Data
public class DataParam {
    /** 应用ID */
    private String appId;
    /** 告警ID列表（多个以逗号分隔） */
    private String alarmIds;
    /** 告警产生时间（UTC） */
    private String alarmGenTimeUTC;
    /** 同步序号 */
    private String syncNO;
    /** 应用名称 */
    private String appName;
    /** 告警名称 */
    private String alarmName;
    /** 清除告警的用户IP */
    private String clearUserIP;
    /** 事件类型 */
    private String eventType;
    /** 流水号 */
    private String serialNO;
    /** 重复次数 */
    private String repeatTimes;
    /** 告警ID */
    private String alarmId;
    /** 清除告警的用户 */
    private String clearUser;
    /** 告警清除类型 */
    private String alarmClearType;
    /** 告警级别 */
    private String alarmLevel;
    /** 告警位置 */
    private String location;
    /** 告警清除时间（UTC） */
    private String alarmClearTimeUTC;
    /** 附加信息 */
    private String appendInfo;
}
