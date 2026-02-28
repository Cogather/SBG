package com.huawei.browsergateway.adapter.dto;

import com.huawei.browsergateway.adapter.interfaces.AlarmAdapter;

import java.util.Map;

/**
 * 告警请求
 */
public class AlarmRequest {
    private String alarmId;
    private AlarmAdapter.AlarmType type;
    private Map<String, String> parameters;
    private Integer maxRetry;
    
    public String getAlarmId() {
        return alarmId;
    }
    
    public void setAlarmId(String alarmId) {
        this.alarmId = alarmId;
    }
    
    public AlarmAdapter.AlarmType getType() {
        return type;
    }
    
    public void setType(AlarmAdapter.AlarmType type) {
        this.type = type;
    }
    
    public Map<String, String> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
    
    public Integer getMaxRetry() {
        return maxRetry;
    }
    
    public void setMaxRetry(Integer maxRetry) {
        this.maxRetry = maxRetry;
    }
}
