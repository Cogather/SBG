package com.huawei.browsergateway.adapter.dto;

import lombok.Data;

/**
 * 告警信息
 */
@Data
public class AlarmInfo {
    private String alarmId;
    private String message;
    private String source;
    private String kind;
    private String name;
    private String namespace;
    private long timestamp;
}
