package com.huawei.browsergateway.entity.event;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 事件信息
 */
@Data
public class EventInfo<T> {
    private String eventCode;
    private String eventType;
    private String userId;
    private String sessionId;
    private T eventData;
    private long timestamp;
    private String level;
    private String source;
    private Map<String, Object> tags;
    
    /**
     * 创建基础事件对象
     */
    public static <T> EventInfo<T> create(String eventCode, String eventType, T data) {
        EventInfo<T> event = new EventInfo<>();
        event.setEventCode(eventCode);
        event.setEventType(eventType);
        event.setEventData(data);
        event.setTimestamp(System.currentTimeMillis());
        event.setLevel("INFO");
        event.setSource("BrowserGateway");
        event.setTags(new HashMap<>());
        return event;
    }
    
    /**
     * 向事件添加标签
     */
    public void addTag(String key, Object value) {
        if (tags == null) {
            tags = new HashMap<>();
        }
        tags.put(key, value);
    }
    
    /**
     * 转换为JSON字符串
     */
    public String toJsonString() {
        // 这里可以使用Jackson或其他JSON库
        // 简化实现，返回基本格式
        return String.format("{\"eventCode\":\"%s\",\"eventType\":\"%s\",\"timestamp\":%d}", 
                eventCode, eventType, timestamp);
    }
}
