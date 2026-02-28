package com.huawei.browsergateway.adapter.dto;

/**
 * 告警信息
 */
public class AlarmInfo {
    private String alarmId;
    private String message;
    private String source;
    private String kind;
    private String name;
    private String namespace;
    private long timestamp;
    
    public String getAlarmId() {
        return alarmId;
    }
    
    public void setAlarmId(String alarmId) {
        this.alarmId = alarmId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public String getKind() {
        return kind;
    }
    
    public void setKind(String kind) {
        this.kind = kind;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getNamespace() {
        return namespace;
    }
    
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
