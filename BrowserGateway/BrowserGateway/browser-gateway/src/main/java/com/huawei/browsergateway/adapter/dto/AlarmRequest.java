package com.huawei.browsergateway.adapter.dto;

import com.huawei.browsergateway.adapter.interfaces.AlarmAdapter;
import lombok.Data;

import java.util.Map;

/**
 * 告警请求
 */
@Data
public class AlarmRequest {
    private String alarmId;
    private AlarmAdapter.AlarmType type;
    private Map<String, String> parameters;
    private Integer maxRetry;
}
